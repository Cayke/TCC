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
#include "jsonHelper.hpp"

namespace server{
    void init (int id, std::string ip, int port, int verbose, std::string cert_path);
    void waitForConnection();
    void error(std::string msg);
    void clientConnected(int socketTCPThread);
    void getRequestStatus(rapidjson::Document *request, int socketTCP);
    void write(rapidjson::Document *request, int socketTCP);
    void readData(rapidjson::Document *request, int socketTCP);
    void readTimestamp(rapidjson::Document *request, int socketTCP);
    void sendResponse(std::string responseJSON, int socketTCP);
}
#endif /* server_hpp */
