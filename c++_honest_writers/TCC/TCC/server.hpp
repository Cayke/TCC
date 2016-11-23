//
//  server.hpp
//  TCC
//
//  Created by Cayke Prudente on 23/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#ifndef server_hpp
#define server_hpp

#include <string>
namespace server{
    void init (int id, std::string ip, int port);
    void waitForConnection();
    void error(std::string msg);
    void clientConnected(int socketTCPThread);
}
#endif /* server_hpp */
