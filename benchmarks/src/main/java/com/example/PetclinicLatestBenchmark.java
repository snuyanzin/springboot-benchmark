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

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Measurement(iterations = 5)
@Warmup(iterations = 1)
@Fork(value = 2, warmups = 0)
@BenchmarkMode(Mode.AverageTime)
public class PetclinicLatestBenchmark {

	private static final String CLASSPATH = "." + File.pathSeparator + "BOOT-INF/classes"
			+ File.pathSeparator + "BOOT-INF/lib/*";

	@Benchmark
	public void fatJar(BasicState state) throws Exception {
		state.run();
	}

	@Benchmark
	public void noverify(NoVerifyState state) throws Exception {
		state.run();
	}

	@Benchmark
	public void explodedJarMain(MainState state) throws Exception {
		state.run();
	}

	@Benchmark
	public void explodedJarFlags(FlagsState state) throws Exception {
		state.run();
	}

	@Benchmark
	public void explodedJarFlagsTiered(FlagsStateTiered state) throws Exception {
		state.run();
	}

	@Benchmark
	public void explodedJarFlagsHeadless(FlagsStateHeadless state) throws Exception {
		state.run();
	}

	@Benchmark
	public void explodedJarFlagsPerfDisabled(FlagsStatePerfDisabled state) throws Exception {
		state.run();
	}

	@Benchmark
	public void avx(AvxState state) throws Exception {
		state.run();
	}

	@Benchmark
	public void allInOne(FlagsStateAllInOne state) throws Exception {
		state.run();
	}

	// @Benchmark
	public void devtoolsRestart(ExplodedDevtoolsState state) throws Exception {
		state.run();
	}

	public static void main(String[] args) throws Exception {
		BasicState state = new BasicState();
		state.run();
	}

	@State(Scope.Benchmark)
	public static class BasicState extends ProcessLauncherState {
		public BasicState() {
			super("target", "-jar",
					jarFile("com.example:petclinic-latest:jar:boot:1.4.2"),
					"--server.port=0");
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class NoVerifyState extends ProcessLauncherState {
		public NoVerifyState() {
			super("target", "-noverify", "-jar",
					jarFile("com.example:petclinic-latest:jar:boot:1.4.2"),
					"--server.port=0");
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class MainState extends ProcessLauncherState {
		public MainState() {
			super("target/demo", "-cp", CLASSPATH,
					"org.springframework.samples.petclinic.PetClinicApplication",
					"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class AvxState extends ProcessLauncherState {
		public AvxState() {
			super("target/demo", "-XX:UseAVX=3", "-cp", CLASSPATH,
				"org.springframework.samples.petclinic.PetClinicApplication",
				"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class FlagsState extends ProcessLauncherState {
		public FlagsState() {
			super("target/demo", "-noverify", "-XX:TieredStopAtLevel=1",
					"-Djava.security.egd=file:/dev/./urandom",
					"-Dspring.config.location=classpath:/application.properties",
					"-Dspring.jmx.enabled=false", "-cp", CLASSPATH,
					"org.springframework.samples.petclinic.PetClinicApplication",
					"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class FlagsStateTiered extends ProcessLauncherState {
		public FlagsStateTiered() {
			super("target/demo", "-noverify", "-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1",
				"-Djava.security.egd=file:/dev/./urandom",
				"-Dspring.config.location=classpath:/application.properties",
				"-Dspring.jmx.enabled=false", "-cp", CLASSPATH,
				"org.springframework.samples.petclinic.PetClinicApplication",
				"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class FlagsStateHeadless extends ProcessLauncherState {
		public FlagsStateHeadless() {
			super("target/demo", "-noverify", "-Djava.awt.headless=true", "-Dlog4j2.disable.jmx=true", "-XX:TieredStopAtLevel=1",
				"-Djava.security.egd=file:/dev/./urandom",
				"-Dspring.config.location=classpath:/application.properties",
				"-Dspring.jmx.enabled=false", "-cp", CLASSPATH,
				"org.springframework.samples.petclinic.PetClinicApplication",
				"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class FlagsStatePerfDisabled extends ProcessLauncherState {
		public FlagsStatePerfDisabled() {
			super("target/demo", "-noverify", "-XX:+PerfDisableSharedMem", "-XX:-UsePerfData", "-XX:TieredStopAtLevel=1",
				"-Djava.security.egd=file:/dev/./urandom",
				"-Dspring.config.location=classpath:/application.properties",
				"-Dspring.jmx.enabled=false", "-cp", CLASSPATH,
				"org.springframework.samples.petclinic.PetClinicApplication",
				"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class FlagsStateAllInOne extends ProcessLauncherState {
		public FlagsStateAllInOne() {
			super("target/demo", "-XX:+AlwaysPreTouch", "-noverify", "-XX:UseAVX=3", "-XX:+PerfDisableSharedMem", "-XX:-UsePerfData",
				"-Djava.awt.headless=true", "-Dlog4j2.disable.jmx=true", "-XX:TieredStopAtLevel=1",
				"-XX:+TieredCompilation",
				"-Djava.security.egd=file:/dev/./urandom",
				"-Dspring.config.location=classpath:/application.properties",
				"-Dspring.jmx.enabled=false", "-cp", CLASSPATH,
				"org.springframework.samples.petclinic.PetClinicApplication",
				"--server.port=0");
			unpack("target/demo", jarFile("com.example:petclinic-latest:jar:boot:1.4.2"));
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

	@State(Scope.Benchmark)
	public static class ExplodedDevtoolsState extends DevToolsLauncherState {

		public ExplodedDevtoolsState() {
			super("target/demo", "/BOOT-INF/classes/.restart",
					jarFile("com.example:petclinic-latest:jar:boot:1.4.2"), "-cp",
					CLASSPATH, "-Dspring.devtools.livereload.enabled=false",
					"-Dspring.devtools.restart.pollInterval=100",
					"-Dspring.devtools.restart.quietPeriod=10",
					"org.springframework.samples.petclinic.PetClinicApplication",
					"--server.port=0");
			try {
				if (Files
						.find(new File("target/demo/BOOT-INF/lib/").toPath(), 1,
								(path, attrs) -> path.getFileName()
										.startsWith("spring-boot-devtools-latest"))
						.count() == 0) {
					copy("target/demo/BOOT-INF/lib",
							"../alt/target/spring-boot-devtools-latest.jar");
				}
			}
			catch (IOException e) {
				throw new IllegalStateException("Failed", e);
			}
		}

		@Override
		@Setup(Level.Trial)
		public void setup() throws Exception {
			super.setup();
		}

		@TearDown(Level.Trial)
		public void stop() throws Exception {
			super.after();
		}
	}

}
