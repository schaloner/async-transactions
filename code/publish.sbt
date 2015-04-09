publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

homepage := Some(url("https://github.com/schaloner/async-transactions"))

licenses := Seq("Apache 2" -> url("http://opensource.org/licenses/Apache-2.0"))

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>git@github.com:schaloner/async-transactions.git</url>
    <connection>scm:git:git@github.com:schaloner/async-transactions.git</connection>
  </scm>
    <developers>
      <developer>
        <id>schaloner</id>
        <name>Steve Chaloner</name>
        <url>http://objectify.be</url>
      </developer>
    </developers>)
