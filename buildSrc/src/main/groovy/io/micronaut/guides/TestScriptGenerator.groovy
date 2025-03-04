package io.micronaut.guides

import groovy.transform.CompileStatic
import io.micronaut.starter.options.BuildTool

import java.util.stream.Collectors

import static io.micronaut.guides.GuideProjectGenerator.DEFAULT_APP_NAME
import static io.micronaut.starter.options.BuildTool.GRADLE
import static io.micronaut.starter.options.BuildTool.MAVEN

@CompileStatic
class TestScriptGenerator {

    public static final String GITHUB_WORKFLOW_JAVA_CI = 'Java CI'
    public static final String ENV_GITHUB_WORKFLOW = 'GITHUB_WORKFLOW'
    public static final String EMPTY_SCRIPT = '''\
#!/usr/bin/env bash
set -e
exit 0
'''

    private static List<String> guidesChanged(List<String> changedFiles) {
        changedFiles.findAll { path ->
            path.startsWith('guides')
        }.collect { path ->
            String guideFolder = path.substring('guides/'.length())
            guideFolder.substring(0, guideFolder.indexOf('/'))
        }.unique()
    }

    private static boolean changesMicronautVersion(List<String> changedFiles) {
        changedFiles.any { it.contains("version.txt") }
    }

    private static boolean changesDependencies(List<String> changedFiles, List<String> changedGuides) {
        if (changedGuides) {
            return false
        }
        changedFiles.any { it.contains("pom.xml") }
    }

    private static boolean changesBuildScr(List<String> changedFiles) {
        changedFiles.any { it.contains('buildSrc') }
    }

    private static boolean shouldSkip(GuideMetadata metadata,
                                      List<String> guidesChanged,
                                      boolean forceExecuteEveryTest) {

        if (!Utils.process(metadata)) {
            return true
        }

        if (forceExecuteEveryTest) {
            return false
        }

        return !guidesChanged.contains(metadata.slug)
    }

    static String generateScript(File guidesFolder,
                                 String metadataConfigName,
                                 boolean stopIfFailure,
                                 List<String> changedFiles) {
        List<String> slugsChanged = guidesChanged(changedFiles)
        boolean forceExecuteEveryTest = changesMicronautVersion(changedFiles) ||
                changesDependencies(changedFiles, slugsChanged) ||
                changesBuildScr(changedFiles) ||
                (System.getenv(ENV_GITHUB_WORKFLOW) && System.getenv(ENV_GITHUB_WORKFLOW) != GITHUB_WORKFLOW_JAVA_CI) ||
                (!changedFiles && !System.getenv(ENV_GITHUB_WORKFLOW))

        List<GuideMetadata> metadatas = GuideProjectGenerator.parseGuidesMetadata(guidesFolder, metadataConfigName)
        metadatas = metadatas.stream()
                .filter(metadata -> !shouldSkip(metadata, slugsChanged, forceExecuteEveryTest))
                .collect(Collectors.toList())
        generateScript(metadatas, stopIfFailure)
    }

    static void generateTestScript(File output,
                                   List<GuideMetadata> metadatas,
                                   boolean stopIfFailure) {
        String script = generateScript(metadatas, stopIfFailure)
        generateTestScript(output, script)
    }

    static void generateTestScript(File output, String script, String scriptFileName = "test.sh") {
        File testScript = new File(output, scriptFileName)
        testScript.createNewFile()
        testScript.text = script
        testScript.executable = true
    }

    static String generateScript(List<GuideMetadata> metadatas,
                                 boolean stopIfFailure) {
        StringBuilder bashScript = new StringBuilder('''\
#!/usr/bin/env bash
set -e

FAILED_PROJECTS=()
EXIT_STATUS=0
''')

        metadatas.sort { it.slug }
        for (GuideMetadata metadata : metadatas) {
            List<GuidesOption> guidesOptionList = GuideProjectGenerator.guidesOptions(metadata)
            bashScript << """\
"""
            for (GuidesOption guidesOption : guidesOptionList) {
                String folder = GuideProjectGenerator.folderName(metadata.slug, guidesOption)
                BuildTool buildTool = folder.contains(MAVEN.toString()) ? MAVEN : GRADLE
                if (buildTool == MAVEN && metadata.skipMavenTests) {
                    continue
                }
                if (buildTool == GRADLE && metadata.skipGradleTests) {
                    continue
                }
                if (metadata.apps.any { it.name == DEFAULT_APP_NAME } ) {
                    bashScript << scriptForFolder(folder, folder, stopIfFailure, buildTool)
                } else {
                    bashScript << """\
cd $folder
"""
                    for (GuideMetadata.App app : metadata.apps) {
                        bashScript << scriptForFolder(app.name, folder + '/' + app.name, stopIfFailure, buildTool)
                    }
                    bashScript << """\
cd ..
"""
                }
            }
        }

        if (!stopIfFailure) {
            bashScript << '''
if [ ${#FAILED_PROJECTS[@]} -ne 0 ]; then
  echo ""
  echo "-------------------------------------------------"
  echo "Projects with errors:"
  for p in `echo ${FAILED_PROJECTS[@]}`; do
    echo "  $p"
  done;
  echo "-------------------------------------------------"
  exit 1
else
  exit 0
fi

'''
        }

        bashScript
    }

    private static String scriptForFolder(String nestedFolder, String folder,
                                          boolean stopIfFailure, BuildTool buildTool) {
        String bashScript = """\
cd $nestedFolder
echo "-------------------------------------------------"
echo "Executing '$folder' tests"
${buildTool == MAVEN ? './mvnw -q test' : './gradlew -q test' } || EXIT_STATUS=\$?
echo "Stopping shared test resources service (if created)"
${buildTool == MAVEN ? './mvnw -q mn:stop-testresources-service' : './gradlew -q stopTestResourcesService'} > /dev/null 2>&1 || true
cd ..
"""
        if (stopIfFailure) {
            bashScript += """\
if [ \$EXIT_STATUS -ne 0 ]; then
  echo "'$folder' tests failed => exit \$EXIT_STATUS"
  exit \$EXIT_STATUS
fi
"""
        } else {
            bashScript += """\
if [ \$EXIT_STATUS -ne 0 ]; then
  FAILED_PROJECTS=("\${FAILED_PROJECTS[@]}" $folder)
  echo "'$folder' tests failed => exit \$EXIT_STATUS"
fi
EXIT_STATUS=0
"""
        }

        bashScript
    }
}
