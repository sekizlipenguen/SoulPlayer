#import "SoulOrientationModule.h"
#import <UIKit/UIKit.h>

@implementation SoulOrientationModule

RCT_EXPORT_MODULE(SoulOrientationModule);

// Dikey moda kilitle
RCT_EXPORT_METHOD(lockToPortrait) {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (@available(iOS 16.0, *)) {
      [self updateOrientationWithMask:UIInterfaceOrientationMaskPortrait];
    } else {
      [self setLegacyOrientation:UIInterfaceOrientationPortrait];
    }
  });
}

// Yatay moda kilitle
RCT_EXPORT_METHOD(lockToLandscape) {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (@available(iOS 16.0, *)) {
      [self updateOrientationWithMask:UIInterfaceOrientationMaskLandscape];
    } else {
      [self setLegacyOrientation:UIInterfaceOrientationLandscapeLeft];
    }
  });
}

// Tüm yönlendirmeleri serbest bırak
RCT_EXPORT_METHOD(unlockAllOrientations) {
  dispatch_async(dispatch_get_main_queue(), ^{
    if (@available(iOS 16.0, *)) {
      [self updateOrientationWithMask:UIInterfaceOrientationMaskAll];
    } else {
      [self setLegacyOrientation:UIInterfaceOrientationUnknown];
    }
  });
}

// iOS 16 ve üzeri için orientation kontrolü
- (void)updateOrientationWithMask:(UIInterfaceOrientationMask)orientationMask API_AVAILABLE(ios(16.0)) {
  if (@available(iOS 16.0, *)) {
    UIWindowScene *windowScene = (UIWindowScene *)[UIApplication sharedApplication].connectedScenes.allObjects.firstObject;
    if ([windowScene isKindOfClass:[UIWindowScene class]]) {
      UIInterfaceOrientationMask interfaceOrientationMask = (UIInterfaceOrientationMask)(orientationMask);
      UIWindowSceneGeometryPreferencesIOS *geometryPreferences = [[UIWindowSceneGeometryPreferencesIOS alloc] initWithInterfaceOrientations:interfaceOrientationMask];
      [windowScene requestGeometryUpdateWithPreferences:geometryPreferences errorHandler:^(NSError * _Nonnull error) {
        NSLog(@"Error updating orientation: %@", error);
      }];
    } else {
      NSLog(@"WindowScene not found or incompatible.");
    }
  }
}

// iOS 13–15 için eski yöntem
- (void)setLegacyOrientation:(UIInterfaceOrientation)orientation {
  [[UIDevice currentDevice] setValue:@(orientation) forKey:@"orientation"];
  [UIViewController attemptRotationToDeviceOrientation];
}

@end
