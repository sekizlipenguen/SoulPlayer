Pod::Spec.new do |s|
  s.name         = "react-native-soul-player"
  s.version      = "1.0.0"
  s.summary      = "A custom React Native video player with casting functionality"
  s.description  = <<-DESC
                   Soul Player: A React Native custom video player with features like AirPlay, Chromecast, and advanced media controls.
                   DESC
  s.homepage     = "https://github.com/yourname/react-native-soul-player"
  s.license      = "MIT"
  s.author       = { "SekizliPenguen" => "https://github.com/sekizlipenguen/react-native-soul-player" }
  s.platform     = :ios, "10.0"
  s.source       = { :git => "https://github.com/yourname/react-native-soul-player.git", :tag => "#{s.version}" }
  s.source_files = "ios/**/*.{h,m,swift}"
  s.requires_arc = true
  s.dependency 'React-Core'
end
