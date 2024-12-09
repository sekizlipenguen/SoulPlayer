#import "CastModule.h"
#import <React/RCTLog.h>
#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <AVKit/AVKit.h>
#import <MediaPlayer/MediaPlayer.h>

@interface CastModule ()
@property (nonatomic, strong) AVRoutePickerView *routePickerView;
@end

@implementation CastModule

RCT_EXPORT_MODULE();

// Desteklenen olaylar
- (NSArray<NSString *> *)supportedEvents {
  return @[@"onAirPlayStart", @"onAirPlayStop"];
}

// Listener eklenirken çağrılır
- (void)startObserving {
  // Burada olaylar için listener kontrol edilebilir
}

- (void)stopObserving {
  // Listener kaldırma işlemi
}

// AirPlay Picker ekranını doğrudan açan metod
RCT_EXPORT_METHOD(showAirPlayPickerDirectly:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  dispatch_async(dispatch_get_main_queue(), ^{
    if (!self.routePickerView) {
      // AVRoutePickerView oluştur
      self.routePickerView = [[AVRoutePickerView alloc] init];
      self.routePickerView.activeTintColor = [UIColor blueColor];
      self.routePickerView.tintColor = [UIColor grayColor];
      self.routePickerView.prioritizesVideoDevices = YES;

      // AirPlay butonunu otomatik tetikle
      for (UIView *subview in self.routePickerView.subviews) {
        if ([subview isKindOfClass:[UIButton class]]) {
          UIButton *airplayButton = (UIButton *)subview;
          [airplayButton sendActionsForControlEvents:UIControlEventTouchUpInside];
          successCallback(@[@"AirPlay picker opened directly"]);
          return;
        }
      }
    }
    errorCallback(@[@"Failed to open AirPlay picker"]);
  });
}

@end
