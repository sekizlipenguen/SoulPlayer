#import "CastModule.h"
#import <React/RCTLog.h>
#import <React/RCTBridgeModule.h>
#import <AVKit/AVKit.h>

@interface CastModule ()
@property (nonatomic, strong) AVPlayer *player;
@property (nonatomic, strong) AVPlayerViewController *playerController;
@end

@implementation CastModule

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(startCasting:(NSString *)url
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  if (url && url.length > 0) {
    NSURL *videoURL = [NSURL URLWithString:url];
    self.player = [AVPlayer playerWithURL:videoURL];
    self.playerController = [[AVPlayerViewController alloc] init];
    self.playerController.player = self.player;

    dispatch_async(dispatch_get_main_queue(), ^{
      UIViewController *rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
      [rootViewController presentViewController:self.playerController animated:YES completion:^{
        [self.player play];
        successCallback(@[@"Casting started successfully"]);
      }];
    });
  } else {
    errorCallback(@[@"Invalid URL"]);
  }
}

RCT_EXPORT_METHOD(seekTo:(nonnull NSNumber *)time
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  if (self.player) {
    CMTime cmTime = CMTimeMakeWithSeconds([time doubleValue], NSEC_PER_SEC);
    [self.player seekToTime:cmTime completionHandler:^(BOOL finished) {
      if (finished) {
        successCallback(@[@"Seek completed"]);
      } else {
        errorCallback(@[@"Seek failed"]);
      }
    }];
  } else {
    errorCallback(@[@"Player not initialized"]);
  }
}

RCT_EXPORT_METHOD(skip:(nonnull NSNumber *)seconds
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  if (self.player) {
    CMTime currentTime = self.player.currentTime;
    CMTime skipTime = CMTimeAdd(currentTime, CMTimeMakeWithSeconds([seconds doubleValue], NSEC_PER_SEC));
    [self.player seekToTime:skipTime completionHandler:^(BOOL finished) {
      if (finished) {
        successCallback(@[@"Skip completed"]);
      } else {
        errorCallback(@[@"Skip failed"]);
      }
    }];
  } else {
    errorCallback(@[@"Player not initialized"]);
  }
}

RCT_EXPORT_METHOD(setPlaybackRate:(nonnull NSNumber *)rate
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  if (self.player) {
    self.player.rate = [rate floatValue];
    successCallback(@[@"Playback rate updated"]);
  } else {
    errorCallback(@[@"Player not initialized"]);
  }
}

RCT_EXPORT_METHOD(changeQuality:(NSString *)bitrate
                  successCallback:(RCTResponseSenderBlock)successCallback
                  errorCallback:(RCTResponseSenderBlock)errorCallback)
{
  if (self.player) {
    NSArray *mediaSelectionOptions = self.player.currentItem.accessLog.events;
    AVMediaSelectionGroup *mediaSelectionGroup = [self.player.currentItem.asset mediaSelectionGroupForMediaCharacteristic:AVMediaCharacteristicVisual];

    for (AVMediaSelectionOption *option in mediaSelectionGroup.options) {
      if ([option.displayName containsString:bitrate]) {
        [self.player.currentItem selectMediaOption:option inMediaSelectionGroup:mediaSelectionGroup];
        successCallback(@[@"Quality changed"]);
        return;
      }
    }
    errorCallback(@[@"Bitrate not available"]);
  } else {
    errorCallback(@[@"Player not initialized"]);
  }
}

@end
