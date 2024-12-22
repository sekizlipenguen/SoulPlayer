require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name           = "react-native-soul-player"
  s.version        = package["version"]
  s.summary        = package["description"]
  s.description    = package["description"]
  s.license        = package["license"]
  s.author         = package["author"]
  s.homepage       = package["homepage"]
  s.source         = { :git => "https://github.com/sekizlipenguen/react-native-soul-player.git", :tag => "v#{s.version}" }
  s.platforms      = { :ios => "13.0" }

  # Kaynak dosyalar
  s.source_files   = "ios/**/*.{h,m,swift}"

  # React Native Bağımlılıkları
  s.dependency "React"
  s.dependency "React-Core"
  s.dependency "React-RCTNetwork"
  s.dependency "React-Codegen"
  s.dependency "RCT-Folly"
  s.dependency "RCTRequired"
  s.dependency "RCTTypeSafety"
  s.dependency "ReactCommon/turbomodule/core"
  # Frameworkler
  s.frameworks = ["AVKit", "MediaPlayer"]
end
