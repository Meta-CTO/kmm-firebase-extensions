Pod::Spec.new do |spec|
    spec.name                     = 'firebase_auth_extensions'
    spec.version                  = '1.0.0'
    spec.homepage                 = 'Link to the Shared Module homepage'
    spec.source                   = { :http=> ''}
    spec.authors                  = ''
    spec.license                  = ''
    spec.summary                  = 'Some description for the Shared Module'
    spec.vendored_frameworks      = 'build/cocoapods/framework/firebase_auth_extensions.framework'
    spec.libraries                = 'c++'
    spec.ios.deployment_target = '14.1'
    spec.dependency 'FirebaseAuth'
    spec.dependency 'GoogleSignIn'
                
    if !Dir.exist?('build/cocoapods/framework/firebase_auth_extensions.framework') || Dir.empty?('build/cocoapods/framework/firebase_auth_extensions.framework')
        raise "

        Kotlin framework 'firebase_auth_extensions' doesn't exist yet, so a proper Xcode project can't be generated.
        'pod install' should be executed after running ':generateDummyFramework' Gradle task:

            ./gradlew :firebase-auth-extensions:generateDummyFramework

        Alternatively, proper pod installation is performed during Gradle sync in the IDE (if Podfile location is set)"
    end
                
    spec.pod_target_xcconfig = {
        'KOTLIN_PROJECT_PATH' => ':firebase-auth-extensions',
        'PRODUCT_MODULE_NAME' => 'firebase_auth_extensions',
    }
                
    spec.script_phases = [
        {
            :name => 'Build firebase_auth_extensions',
            :execution_position => :before_compile,
            :shell_path => '/bin/sh',
            :script => <<-SCRIPT
                if [ "YES" = "$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                  exit 0
                fi
                set -ev
                REPO_ROOT="$PODS_TARGET_SRCROOT"
                "$REPO_ROOT/../gradlew" -p "$REPO_ROOT" $KOTLIN_PROJECT_PATH:syncFramework \
                    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
                    -Pkotlin.native.cocoapods.archs="$ARCHS" \
                    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
            SCRIPT
        }
    ]
                
end