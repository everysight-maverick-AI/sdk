"""Created by Everysight LTD.

Xcode project template for new_mav2_project.py.

The object IDs are fixed and hand-assigned rather than random: it keeps the generated
file diffable and reviewable, and Xcode only requires them to be unique within the
file. Adapted from the ios-native sample so the generated project matches what the
build system already validates on every SDK release.

Placeholders: {target_name} {bundle_id} {spm_url} {spm_requirement} {deployment_target}
              {development_team_line}
"""

PBXPROJ = '''\
// !$*UTF8*$!
{{
	archiveVersion = 1;
	classes = {{
	}};
	objectVersion = 60;
	objects = {{

/* Begin PBXBuildFile section */
		A00000010000000000000001 /* AppDelegate.swift in Sources */ = {{isa = PBXBuildFile; fileRef = A00000110000000000000001 /* AppDelegate.swift */; }};
		A00000020000000000000001 /* ViewController.swift in Sources */ = {{isa = PBXBuildFile; fileRef = A00000120000000000000001 /* ViewController.swift */; }};
		A00000060000000000000001 /* MaverickAI in Frameworks */ = {{isa = PBXBuildFile; productRef = A00000710000000000000001 /* MaverickAI */; }};
		A00000070000000000000001 /* sdk.key in Resources */ = {{isa = PBXBuildFile; fileRef = A00000150000000000000001 /* sdk.key */; }};
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		A00000100000000000000001 /* {target_name}.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; path = {target_name}.app; sourceTree = BUILT_PRODUCTS_DIR; }};
		A00000110000000000000001 /* AppDelegate.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = AppDelegate.swift; sourceTree = "<group>"; }};
		A00000120000000000000001 /* ViewController.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = ViewController.swift; sourceTree = "<group>"; }};
		A00000140000000000000001 /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; }};
		A00000150000000000000001 /* sdk.key */ = {{isa = PBXFileReference; explicitFileType = compiled; path = sdk.key; sourceTree = "<group>"; }};
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		A00000200000000000000001 /* Frameworks */ = {{
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
				A00000060000000000000001 /* MaverickAI in Frameworks */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		A00000300000000000000001 = {{
			isa = PBXGroup;
			children = (
				A00000310000000000000001 /* {target_name} */,
				A00000320000000000000001 /* Frameworks */,
				A00000330000000000000001 /* Products */,
			);
			sourceTree = "<group>";
		}};
		A00000310000000000000001 /* {target_name} */ = {{
			isa = PBXGroup;
			children = (
				A00000150000000000000001 /* sdk.key */,
				A00000110000000000000001 /* AppDelegate.swift */,
				A00000120000000000000001 /* ViewController.swift */,
				A00000140000000000000001 /* Info.plist */,
			);
			path = {target_name};
			sourceTree = "<group>";
		}};
		A00000320000000000000001 /* Frameworks */ = {{
			isa = PBXGroup;
			children = (
			);
			name = Frameworks;
			sourceTree = "<group>";
		}};
		A00000330000000000000001 /* Products */ = {{
			isa = PBXGroup;
			children = (
				A00000100000000000000001 /* {target_name}.app */,
			);
			name = Products;
			sourceTree = "<group>";
		}};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		A00000400000000000000001 /* {target_name} */ = {{
			isa = PBXNativeTarget;
			buildConfigurationList = A00000530000000000000001 /* Build configuration list for PBXNativeTarget "{target_name}" */;
			buildPhases = (
				A00000810000000000000001 /* Check SDK key */,
				A00000210000000000000001 /* Sources */,
				A00000200000000000000001 /* Frameworks */,
				A00000220000000000000001 /* Resources */,
				A00000800000000000000001 /* Copy Maverick AI Compose Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = {target_name};
			packageProductDependencies = (
				A00000710000000000000001 /* MaverickAI */,
			);
			productName = {target_name};
			productReference = A00000100000000000000001 /* {target_name}.app */;
			productType = "com.apple.product-type.application";
		}};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		A00000500000000000000001 /* Project object */ = {{
			isa = PBXProject;
			attributes = {{
				BuildIndependentTargetsInParallel = 1;
				LastUpgradeCheck = 1600;
				TargetAttributes = {{
					A00000400000000000000001 = {{
						CreatedOnToolsVersion = 16.0;
					}};
				}};
			}};
			buildConfigurationList = A00000520000000000000001 /* Build configuration list for PBXProject "{target_name}" */;
			compatibilityVersion = "Xcode 15.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = A00000300000000000000001;
			packageReferences = (
				A00000700000000000000001 /* XCRemoteSwiftPackageReference "mav-ai-ios-spm" */,
			);
			productRefGroup = A00000330000000000000001 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				A00000400000000000000001 /* {target_name} */,
			);
		}};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		A00000220000000000000001 /* Resources */ = {{
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				A00000070000000000000001 /* sdk.key in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		A00000810000000000000001 /* Check SDK key */ = {{
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Check SDK key";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "KEY=\\"${{SRCROOT}}/{target_name}/sdk.key\\"\\nif [ ! -f \\"$KEY\\" ]; then\\n  echo \\"error: no Everysight key bundled. Put the key issued to you at {target_name}/sdk.key - see README.md. If you were issued app.key, rename it to sdk.key or add it to the target yourself. Without a key doInit() still succeeds, but connecting ends in AuthFailed.\\"\\n  exit 1\\nfi\\nif [ ! -s \\"$KEY\\" ]; then\\n  echo \\"error: {target_name}/sdk.key is empty. Replace it with the key issued by Everysight.\\"\\n  exit 1\\nfi\\n";
		}};
		A00000800000000000000001 /* Copy Maverick AI Compose Resources */ = {{
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Copy Maverick AI Compose Resources";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "# The SDK ships its Compose resources inside the framework bundle. iOS does not\\n# look for them there, so copy them up into the .app or SDK images and fonts will\\n# be missing at runtime. Do not remove this phase.\\nfor FW in \\"${{TARGET_BUILD_DIR}}/${{PRODUCT_NAME}}.app/Frameworks/\\"*.framework; do\\n    if [ -d \\"$FW/compose-resources\\" ]; then\\n        rsync -av \\"$FW/compose-resources\\" \\\\\\n            \\"${{TARGET_BUILD_DIR}}/${{PRODUCT_NAME}}.app/\\"\\n    fi\\ndone\\n";
		}};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		A00000210000000000000001 /* Sources */ = {{
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				A00000010000000000000001 /* AppDelegate.swift in Sources */,
				A00000020000000000000001 /* ViewController.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		A00000600000000000000001 /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_DYNAMIC_NO_PIC = NO;
				GCC_OPTIMIZATION_LEVEL = 0;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
			}};
			name = Debug;
		}};
		A00000610000000000000001 /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_OPTIMIZATION_LEVEL = 3;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				SDKROOT = iphoneos;
			}};
			name = Release;
		}};
		A00000620000000000000001 /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
{development_team_line}				"EXCLUDED_ARCHS[sdk=iphonesimulator*]" = x86_64;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = {target_name}/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = {bundle_id};
				PRODUCT_NAME = "$(TARGET_NAME)";
				SDKROOT = iphoneos;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO;
				SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Debug;
		}};
		A00000630000000000000001 /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_STYLE = Automatic;
{development_team_line}				"EXCLUDED_ARCHS[sdk=iphonesimulator*]" = x86_64;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = {target_name}/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = {bundle_id};
				PRODUCT_NAME = "$(TARGET_NAME)";
				SDKROOT = iphoneos;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SUPPORTS_MAC_DESIGNED_FOR_IPHONE_IPAD = NO;
				SUPPORTS_XR_DESIGNED_FOR_IPHONE_IPAD = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Release;
		}};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		A00000520000000000000001 /* Build configuration list for PBXProject "{target_name}" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				A00000600000000000000001 /* Debug */,
				A00000610000000000000001 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
		A00000530000000000000001 /* Build configuration list for PBXNativeTarget "{target_name}" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				A00000620000000000000001 /* Debug */,
				A00000630000000000000001 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
/* End XCConfigurationList section */

/* Begin XCRemoteSwiftPackageReference section */
		A00000700000000000000001 /* XCRemoteSwiftPackageReference "mav-ai-ios-spm" */ = {{
			isa = XCRemoteSwiftPackageReference;
			repositoryURL = "{spm_url}";
			requirement = {{
{spm_requirement}			}};
		}};
/* End XCRemoteSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
		A00000710000000000000001 /* MaverickAI */ = {{
			isa = XCSwiftPackageProductDependency;
			package = A00000700000000000000001 /* XCRemoteSwiftPackageReference "mav-ai-ios-spm" */;
			productName = MaverickAI;
		}};
/* End XCSwiftPackageProductDependency section */
	}};
	rootObject = A00000500000000000000001 /* Project object */;
}}
'''

XCSCHEME = '''\
<?xml version="1.0" encoding="UTF-8"?>
<Scheme LastUpgradeVersion="1600" version="1.7">
   <BuildAction parallelizeBuildables="YES" buildImplicitDependencies="YES">
      <BuildActionEntries>
         <BuildActionEntry buildForTesting="YES" buildForRunning="YES" buildForProfiling="YES" buildForArchiving="YES" buildForAnalyzing="YES">
            <BuildableReference
               BuildableIdentifier="primary"
               BlueprintIdentifier="A00000400000000000000001"
               BuildableName="{target_name}.app"
               BlueprintName="{target_name}"
               ReferencedContainer="container:{target_name}.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" shouldUseLaunchSchemeArgsEnv="YES">
      <Testables>
      </Testables>
   </TestAction>
   <LaunchAction buildConfiguration="Debug" selectedDebuggerIdentifier="Xcode.DebuggerFoundation.Debugger.LLDB" selectedLauncherIdentifier="Xcode.DebuggerFoundation.Launcher.LLDB" launchStyle="0" useCustomWorkingDirectory="NO" ignoresPersistentStateOnLaunch="NO" debugDocumentVersioning="YES" debugServiceExtension="internal" allowLocationSimulation="YES">
      <BuildableProductRunnable runnableDebuggingMode="0">
         <BuildableReference
            BuildableIdentifier="primary"
            BlueprintIdentifier="A00000400000000000000001"
            BuildableName="{target_name}.app"
            BlueprintName="{target_name}"
            ReferencedContainer="container:{target_name}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction buildConfiguration="Release" shouldUseLaunchSchemeArgsEnv="YES" savedToolIdentifier="" useCustomWorkingDirectory="NO" debugDocumentVersioning="YES">
      <BuildableProductRunnable runnableDebuggingMode="0">
         <BuildableReference
            BuildableIdentifier="primary"
            BlueprintIdentifier="A00000400000000000000001"
            BuildableName="{target_name}.app"
            BlueprintName="{target_name}"
            ReferencedContainer="container:{target_name}.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction buildConfiguration="Debug">
   </AnalyzeAction>
   <ArchiveAction buildConfiguration="Release" revealArchiveInOrganizer="YES">
   </ArchiveAction>
</Scheme>
'''


# ── Compose Multiplatform iosApp ─────────────────────────────────────────────
#
# The Kotlin side is compiled by Gradle, not Xcode: the first build phase runs
# embedAndSignAppleFrameworkForXcode, which builds ComposeApp.framework for the SDK and
# configuration Xcode is building and drops it under composeApp/build/xcode-frameworks,
# where FRAMEWORK_SEARCH_PATHS finds it. syncComposeResourcesForIos copies the Compose and
# SDK resources into the .app - the job the native project's rsync phase does by hand.
#
# The native target id is the same as the native project's, so XCSCHEME serves both.

KMP_PBXPROJ = '''\
// !$*UTF8*$!
{{
	archiveVersion = 1;
	classes = {{
	}};
	objectVersion = 60;
	objects = {{

/* Begin PBXBuildFile section */
		C00000010000000000000001 /* iOSApp.swift in Sources */ = {{isa = PBXBuildFile; fileRef = C00000110000000000000001 /* iOSApp.swift */; }};
		C00000020000000000000001 /* sdk.key in Resources */ = {{isa = PBXBuildFile; fileRef = C00000120000000000000001 /* sdk.key */; }};
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
		C00000100000000000000001 /* {target_name}.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = {target_name}.app; sourceTree = BUILT_PRODUCTS_DIR; }};
		C00000110000000000000001 /* iOSApp.swift */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = iOSApp.swift; sourceTree = "<group>"; }};
		C00000120000000000000001 /* sdk.key */ = {{isa = PBXFileReference; lastKnownFileType = text; path = sdk.key; sourceTree = "<group>"; }};
		C00000130000000000000001 /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; }};
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		C00000200000000000000001 /* Frameworks */ = {{
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		C00000300000000000000001 = {{
			isa = PBXGroup;
			children = (
				C00000310000000000000001 /* {target_name} */,
				C00000330000000000000001 /* Products */,
			);
			sourceTree = "<group>";
		}};
		C00000310000000000000001 /* {target_name} */ = {{
			isa = PBXGroup;
			children = (
				C00000110000000000000001 /* iOSApp.swift */,
				C00000120000000000000001 /* sdk.key */,
				C00000130000000000000001 /* Info.plist */,
			);
			path = {target_name};
			sourceTree = "<group>";
		}};
		C00000330000000000000001 /* Products */ = {{
			isa = PBXGroup;
			children = (
				C00000100000000000000001 /* {target_name}.app */,
			);
			name = Products;
			sourceTree = "<group>";
		}};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		A00000400000000000000001 /* {target_name} */ = {{
			isa = PBXNativeTarget;
			buildConfigurationList = C00000530000000000000001 /* Build configuration list for PBXNativeTarget "{target_name}" */;
			buildPhases = (
				C00000700000000000000001 /* Compile Kotlin Framework */,
				C00000210000000000000001 /* Sources */,
				C00000200000000000000001 /* Frameworks */,
				C00000220000000000000001 /* Resources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = {target_name};
			productName = {target_name};
			productReference = C00000100000000000000001 /* {target_name}.app */;
			productType = "com.apple.product-type.application";
		}};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		C00000500000000000000001 /* Project object */ = {{
			isa = PBXProject;
			attributes = {{
				BuildIndependentTargetsInParallel = 1;
				LastUpgradeCheck = 1600;
				TargetAttributes = {{
					A00000400000000000000001 = {{
						CreatedOnToolsVersion = 16.0;
					}};
				}};
			}};
			buildConfigurationList = C00000520000000000000001 /* Build configuration list for PBXProject "{target_name}" */;
			compatibilityVersion = "Xcode 15.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = C00000300000000000000001;
			productRefGroup = C00000330000000000000001 /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				A00000400000000000000001 /* {target_name} */,
			);
		}};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		C00000220000000000000001 /* Resources */ = {{
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				C00000020000000000000001 /* sdk.key in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		C00000700000000000000001 /* Compile Kotlin Framework */ = {{
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputFileListPaths = (
			);
			inputPaths = (
			);
			name = "Compile Kotlin Framework";
			outputFileListPaths = (
			);
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "cd \\"$SRCROOT/..\\"\\nif [ ! -x ./gradlew ]; then\\n  echo \\"error: no Gradle wrapper in $(pwd). Run once: gradle wrapper --gradle-version {gradle_version} (see README.md)\\"\\n  exit 1\\nfi\\nif [ \\"YES\\" = \\"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\\" ]; then\\n  echo \\"Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES\\"\\n  exit 0\\nfi\\nSDK_VERSION_ARG=\\"\\"\\nif [ -n \\"$MAV2_SDK_VERSION\\" ]; then\\n  SDK_VERSION_ARG=\\"-PsdkVersion=$MAV2_SDK_VERSION\\"\\nfi\\n./gradlew $SDK_VERSION_ARG :composeApp:embedAndSignAppleFrameworkForXcode :composeApp:syncComposeResourcesForIos\\n";
		}};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		C00000210000000000000001 /* Sources */ = {{
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				C00000010000000000000001 /* iOSApp.swift in Sources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		C00000600000000000000001 /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_OPTIMIZATION_LEVEL = 0;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
			}};
			name = Debug;
		}};
		C00000610000000000000001 /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = "dwarf-with-dsym";
				ENABLE_USER_SCRIPT_SANDBOXING = NO;
				GCC_C_LANGUAGE_STANDARD = gnu17;
				GCC_OPTIMIZATION_LEVEL = s;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				SDKROOT = iphoneos;
			}};
			name = Release;
		}};
		C00000620000000000000001 /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				CODE_SIGN_STYLE = Automatic;
{development_team_line}				"EXCLUDED_ARCHS[sdk=iphonesimulator*]" = x86_64;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = {target_name}/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = {bundle_id};
				PRODUCT_NAME = "$(TARGET_NAME)";
				SDKROOT = iphoneos;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Debug;
		}};
		C00000630000000000000001 /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				CODE_SIGN_STYLE = Automatic;
{development_team_line}				"EXCLUDED_ARCHS[sdk=iphonesimulator*]" = x86_64;
				FRAMEWORK_SEARCH_PATHS = (
					"$(inherited)",
					"$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
				);
				INFOPLIST_FILE = {target_name}/Info.plist;
				IPHONEOS_DEPLOYMENT_TARGET = {deployment_target};
				LD_RUNPATH_SEARCH_PATHS = (
					"$(inherited)",
					"@executable_path/Frameworks",
				);
				PRODUCT_BUNDLE_IDENTIFIER = {bundle_id};
				PRODUCT_NAME = "$(TARGET_NAME)";
				SDKROOT = iphoneos;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Release;
		}};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		C00000520000000000000001 /* Build configuration list for PBXProject "{target_name}" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				C00000600000000000000001 /* Debug */,
				C00000610000000000000001 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
		C00000530000000000000001 /* Build configuration list for PBXNativeTarget "{target_name}" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				C00000620000000000000001 /* Debug */,
				C00000630000000000000001 /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
/* End XCConfigurationList section */
	}};
	rootObject = C00000500000000000000001 /* Project object */;
}}
'''
