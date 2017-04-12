

#import <Foundation/Foundation.h>

typedef void (^AirKissConnectionSuccess) (void);
typedef void (^AirKissConnectionFailure) (void);

@interface AirKissConnection : NSObject

@property(nonatomic,copy) AirKissConnectionSuccess connectionSuccess;
@property(nonatomic,copy) AirKissConnectionFailure connectionFailure;

/**
 *  AirKiss连接
 *
 *  @param ssidStr ssid
 *  @param pswStr  psw
 */
- (void)connectAirKissWithSSID:(NSString *)ssidStr
                      password:(NSString *)password;

- (void)closeConnection;

@end
