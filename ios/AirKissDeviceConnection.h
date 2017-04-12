

#import <Foundation/Foundation.h>

typedef void (^AirKissConnectionDSuccess) (NSString*);
typedef void (^AirKissConnectionDFailure) (void);

@interface AirKissConnectionD : NSObject

@property(nonatomic,copy) AirKissConnectionDSuccess connectionSuccess;
@property(nonatomic,copy) AirKissConnectionDFailure connectionFailure;


- (void)connectAirKissDevice;

- (void)closeConnection;

@end
