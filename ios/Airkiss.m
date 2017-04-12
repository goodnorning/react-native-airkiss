

#import "Airkiss.h"
#import <React/RCTAssert.h>
#import <React/RCTBridge.h>
#import <React/RCTConvert.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTLog.h>
#import "AirKissConnection.h"
#import "AirKissDeviceConnection.h"

@interface Airkiss() {
    AirKissConnection *_airKissConnection;
    AirKissConnectionD *_airKissConnectionD;
}
@end

@implementation Airkiss


RCT_EXPORT_MODULE()
@synthesize bridge = _bridge;

RCT_EXPORT_METHOD(start:(NSString*)ssid password:(NSString*)password callback:(RCTResponseSenderBlock)callback)
{
    if (!ssid || !password) {
        RCTLogError(@"%@.start with nil ssid or password.", [self class]);
        return;
    }
    if (!_airKissConnection) {
        _airKissConnection = [[AirKissConnection alloc] init];
        _airKissConnection.connectionSuccess = ^() {
            NSMutableDictionary *dic = [NSMutableDictionary new];
            [dic setObject:@(1) forKey:@"code"];
            callback(dic);
        };
        
        _airKissConnection.connectionFailure = ^() {
            NSMutableDictionary *dic = [NSMutableDictionary new];
            [dic setObject:@(-1) forKey:@"code"];
            callback(dic);
        };
    }
    
    [_airKissConnection connectAirKissWithSSID:ssid password:password];
}

RCT_EXPORT_METHOD(startGetDeviceInfo:(RCTResponseSenderBlock)callback)
{
    if (!_airKissConnectionD) {
        _airKissConnectionD = [[AirKissConnectionD alloc] init];
        _airKissConnectionD.connectionSuccess = ^(ret) {
            NSMutableDictionary *dic = [NSMutableDictionary new];
            [dic setObject:@(1) forKey:@"code"];
            [dic setObject:ret forKey:@"device"];
            callback(dic);
        };
        
        _airKissConnectionD.connectionFailure = ^() {
            NSMutableDictionary *dic = [NSMutableDictionary new];
            [dic setObject:@(-1) forKey:@"code"];
            callback(dic);
        };
    }
    
    [_airKissConnectionD connectAirKissDevice];
}

RCT_EXPORT_METHOD(stop)
{
    if (_airKissConnection)
        [_airKissConnection closeConnection];
    if (_airKissConnectionD)
        [_airKissConnectionD closeConnection];
}

@end
