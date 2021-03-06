

#import "AirKissConnection.h"
#import "AirKissEncoder.h"
#import "GCDAsyncUdpSocket.h"

#define kAirKiss_Port                    10000
#define kAirKiss_Host                    @"255.255.255.255"
#define kAirKiss_Limit_Return_Random_Num 5

@interface AirKissConnection()<GCDAsyncUdpSocketDelegate>
{
    AirKissEncoder  *_airKissEncoder;
    NSTimer           *_timer;          // 超过1分钟未连接成功则表示失败
    
    GCDAsyncUdpSocket *_clientUdpSocket;
    GCDAsyncUdpSocket *_serverUdpSocket;
    
    long              _tag;
    int               _returnRandomNum;
    
    BOOL              _airkissDone;
    BOOL              _callConnectFailed;
}
@end

@implementation AirKissConnection

- (instancetype)init
{
    self = [super init];
    if (self) {
        _airKissEncoder  = [[AirKissEncoder alloc] init];
        _tag             = 0;
        _returnRandomNum = 0;
        _airkissDone = false;
        _callConnectFailed = false;
        [self setupClientUdpSocket];
        [self setupServerUdpSocket];
    }
    return self;
}

#pragma mark - Connection
/**
 *  AirKiss连接
 *
 *  @param ssidStr ssid
 *  @param pswStr  psw
 */
- (void)connectAirKissWithSSID:(NSString *)ssidStr
                      password:(NSString *)password {
    NSMutableArray *dataArray = [_airKissEncoder createAirKissEncorderWithSSID:ssidStr ? :@""
                                                                      password:password ? :@""];

    _tag                      = 0;
    _returnRandomNum          = 0;
    _callConnectFailed           = false;
    _airkissDone = false;
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        dispatch_async(dispatch_get_main_queue(), ^{
            _timer = [NSTimer scheduledTimerWithTimeInterval:60
                                                      target:self
                                                    selector:@selector(connectFailure)
                                                    userInfo:nil
                                                     repeats:NO];
        });
        
        for (int i = 0;i < dataArray.count;i++) {
            if (_airkissDone == true) {
                break;
            }
            UInt16   length      = [dataArray[i] unsignedShortValue];
            NSMutableData *mData = [NSMutableData data];
            UInt8    value       = 0;
            for (int j = 0; j < length; j++) {
                [mData appendBytes:&value length:1];
            }
            [_clientUdpSocket sendData:mData
                                toHost:kAirKiss_Host
                                  port:kAirKiss_Port
                           withTimeout:-1
                                   tag:_tag];
            
            [NSThread sleepForTimeInterval:0.004];
            
            _tag++;
        }
        if (!_airkissDone && _connectionFailure && !_callConnectFailed) {
            _callConnectFailed = true;
            _connectionFailure();
        }
    });
}

- (void)closeConnection {
    
    [_timer invalidate];
    _timer = nil;
    
    [_clientUdpSocket close];
    [_serverUdpSocket close];
    
    _clientUdpSocket = nil;
    _serverUdpSocket = nil;
}

#pragma mark - 连接失败
- (void)connectFailure {
    [self closeConnection];
    if (_connectionFailure && !_airkissDone && !_callConnectFailed) {
        _callConnectFailed = true;
        _connectionFailure();
    }
}

#pragma mark - Set up udp socket
- (void)setupClientUdpSocket
{
    NSError *error = nil;
    
    if (!_clientUdpSocket) {
        _clientUdpSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_main_queue()];
        [_clientUdpSocket enableBroadcast:YES error:&error];
    }
    
    if (![_clientUdpSocket bindToPort:0 error:&error])
    {
        return;
    }
    
    if (![_clientUdpSocket beginReceiving:&error])
    {
        return;
    }
}

- (void)setupServerUdpSocket {
    if (!_serverUdpSocket) {
        _serverUdpSocket = [[GCDAsyncUdpSocket alloc] initWithDelegate:self delegateQueue:dispatch_get_main_queue()];
        [_serverUdpSocket enableBroadcast:YES error:nil];
    }
    
    NSError *error = nil;
    
    if (![_serverUdpSocket bindToPort:kAirKiss_Port error:&error])
    {
        return;
    }
    
    if (![_serverUdpSocket beginReceiving:&error])
    {
        return;
    }
}

#pragma mark - GCDAsyncUdpSocketDelegate
- (void)udpSocket:(GCDAsyncUdpSocket *)sock didSendDataWithTag:(long)tag
{
    // You could add checks here
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock didNotSendDataWithTag:(long)tag dueToError:(NSError *)error
{
    // You could add checks here
}

- (void)udpSocket:(GCDAsyncUdpSocket *)sock
   didReceiveData:(NSData *)data
      fromAddress:(NSData *)address
withFilterContext:(id)filterContext
{
    if (_serverUdpSocket == sock) {
        if (_airkissDone) {
            return;
        }

        // 设备连接WIFI成功后会像10000端口发送至少20个UDP广播包所附带的随机数
        UInt8 *bytes = (UInt8 *) [data bytes];
        for (int i=0;i<data.length;i++) {
            if (bytes[i]==_airKissEncoder.randomChar) {
                _returnRandomNum++;
                if (_returnRandomNum >= kAirKiss_Limit_Return_Random_Num) {
                    // 成功
                    _airkissDone = true;
                    [self closeConnection];
                    if (_connectionSuccess) {
                        _connectionSuccess();
                    }
                }
            }
        }
    }
}

@end
