/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import org.openjdk.jmh.util.FileUtils;
import org.openjdk.jmh.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcessLauncherState {

	private Process started;
	private List<String> args;
	private File home;

	public ProcessLauncherState(String dir, String... args) {
		this.args = new ArrayList<>();
		this.args.add(System.getProperty("java.home") + "/bin/java");
		this.args.add("-Xms1024m");
		this.args.add("-Xmx1024m");
		this.args.add("-Djava.security.egd=file:/dev/./urandom");
		String vendor = System.getProperty("java.vendor", "").toLowerCase();
		if (vendor.contains("ibm") || vendor.contains("j9")) {
			this.args.addAll(Arrays.asList("-Xms32m", "-Xquickstart", "-Xshareclasses",
					"-Xscmx128m"));
		}
		else {
			this.args.addAll(Arrays.asList("-XX:TieredStopAtLevel=1"));
		}
		this.args.addAll(Arrays.asList(args));
		if (System.getProperty("bench.args") != null) {
			this.args.addAll(4,
					Arrays.asList(System.getProperty("bench.args").split(" ")));
		}
		this.home = new File(dir);
	}

	public void after() throws Exception {
		if (started != null && started.isAlive()) {
			started.destroyForcibly().waitFor();
		}
	}

	public void replace(String pattern, String value) {
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i).replace(pattern, value);
			args.set(i, arg);
		}
	}

	public Collection<String> capture(String... additional) throws Exception {
		after();
		List<String> args = new ArrayList<>(this.args);
		args.addAll(Arrays.asList(additional));
		ProcessBuilder builder = new ProcessBuilder(args);
		builder.directory(home);
		builder.redirectErrorStream(true);
		customize(builder);
		if (!"false".equals(System.getProperty("debug", "false"))) {
			System.err.println("Running: " + Utils.join(args, " "));
		}
		started = builder.start();
		started.waitFor();
		return FileUtils.readAllLines(started.getInputStream());
	}

	public void run() throws Exception {
		after();
		ProcessBuilder builder = new ProcessBuilder(args);
		builder.directory(home);
		builder.redirectErrorStream(true);
		customize(builder);
		if (!"false".equals(System.getProperty("debug", "false"))) {
			System.err.println("Running: " + Utils.join(args, " "));
		}

		started = builder.start();
		monitor();
	}

	protected void customize(ProcessBuilder builder) {
	}

	protected void monitor() throws IOException {
		System.out.println(output(started.getInputStream(), "Started"));
	}

	protected static String output(InputStream inputStream, String marker)
			throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader br = null;
		br = new BufferedReader(new InputStreamReader(inputStream));
		String line = null;
		while ((line = br.readLine()) != null && !line.contains(marker)) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		if (line != null) {
			sb.append(line).append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	public void copy(String path, String jar) {
		File dest = new File(path);
		dest.mkdirs();
		try {
			File file = new File(jar);
			Files.copy(file.toPath(), dest.toPath().resolve(file.getName()));
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed", e);
		}
	}

	public void unpack(String path, String jar) {
		File home = new File(path);
		ProcessBuilder builder = new ProcessBuilder(getJarExec(), "xf", jar);
		Process started = null;
		try {
			if (home.exists()) {
				Files.walkFileTree(home.toPath(), new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc)
							throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
				if (home.exists()) {
					Files.delete(home.toPath());
				}
			}
			home.mkdirs();
			builder.directory(home);
			builder.redirectErrorStream(true);
			started = builder.start();
			started.waitFor();
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed", e);
		}
		finally {
			if (started != null && started.isAlive()) {
				started.destroy();
			}
		}
	}

	private String getJarExec() {
		String home = System.getProperty("java.home");
		String jar = home + "/../bin/jar";
		if (new File(jar).exists()) {
			return jar;
		}
		jar = home + "/../bin/jar.exe";
		if (new File(jar).exists()) {
			return jar;
		}
		return home + "/bin/jar";
	}

	public static String jarFile(String coordinates) {
		Pattern p = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]*)(:([^: ]+))?)?:([^: ]+)");
		Matcher m = p.matcher(coordinates);
		if (!m.matches()) {
			throw new IllegalArgumentException("Bad artifact coordinates " + coordinates
					+ ", expected format is <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>");
		}
		String artifactId = m.group(2);
		String extension = get(m.group(4), "jar");
		String classifier = get(m.group(6), null);
		classifier = classifier == null ? "" : "-" + classifier;
		String version = m.group(7);
		String path = ".."; // always run in benchmarks folder
		try {
			return new File(path).getAbsoluteFile().getCanonicalPath() + File.separator
					+ artifactId + File.separator + "target" + File.separator + artifactId
					+ "-" + version + classifier + "." + extension;
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot find benchmarks", e);
		}
	}

	private static String get(String value, String defaultValue) {
		return (value == null || value.length() <= 0) ? defaultValue : value;
	}

	public File getHome() {
		return home;
	}
}