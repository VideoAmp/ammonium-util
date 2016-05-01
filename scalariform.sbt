import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform, SbtScalariform.ScalariformKeys

disablePlugins(SbtScalariform)

SbtScalariform.defaultScalariformSettings

ScalariformKeys.preferences :=
  ScalariformKeys.preferences.value
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(CompactControlReadability, true)
    .setPreference(PreserveSpaceBeforeArguments, true)
