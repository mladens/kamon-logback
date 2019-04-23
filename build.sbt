/* =========================================================================================
 * Copyright © 2013-2018 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */

val kamonCore             = "io.kamon"        %%  "kamon-core"              % "2.0.0-07ebec0a2acd1ecd22a6cf41cb770e8daf39e3cc"
val kamonTestkit          = "io.kamon"        %%  "kamon-testkit"           % "2.0.0-07ebec0a2acd1ecd22a6cf41cb770e8daf39e3cc"
val kanelaScalaExtension  = "io.kamon"        %%  "kanela-scala-extension"  % "0.0.14"
val kanelaAgent           = "io.kamon"        %   "kanela-agent"            % "0.0.21-SNAPSHOT" changing()

val logbackClassic  = "ch.qos.logback"  %   "logback-classic"    % "1.2.3"

resolvers += Resolver.bintrayRepo("kamon-io", "snapshots")
resolvers += Resolver.mavenLocal

lazy val root = (project in file("."))
  .settings(Seq(
      name := "kamon-logback",
      scalaVersion := "2.12.8"))
  .enablePlugins(JavaAgent)
  .settings(javaAgents += "io.kamon" % "kanela-agent" % "0.0.19-SNAPSHOT" % "compile;test")
  .settings(
    libraryDependencies ++=
      compileScope(kamonCore, logbackClassic, kanelaScalaExtension, kanelaAgent) ++
      testScope(kamonTestkit, scalatest))


