

#import <Foundation/Foundation.h>

@interface AirKissEncoder : NSObject
/**
 *  创建AirKiss数据
 *
 *  @param ssid <#ssid description#>
 *  @param psd  <#psd description#>
 *
 *  @return 装载要发送的每条数据的长度的数组
 */
- (NSMutableArray *)createAirKissEncorderWithSSID:(NSString *)ssid
                                         password:(NSString *)password;

/**
 *@description 随机数
 */
@property (nonatomic,readonly) UInt8 randomChar;

@end
