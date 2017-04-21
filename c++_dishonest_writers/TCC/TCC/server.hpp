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
    void init (int id, std::string ip, int port);
    void waitForConnection();
    void error(std::string msg);
    void clientConnected(int socketTCPThread);
    void getRequestStatus(rapidjson::Document *request, int socketTCP);
    void write(rapidjson::Document *request, int socketTCP, std::string type);
    void readData(rapidjson::Document *request, int socketTCP);
    void readTimestamp(rapidjson::Document *request, int socketTCP);
    void sendResponse(std::string responseJSON, int socketTCP);
    void getEchoe(rapidjson::Document *request, int socketTCP);
    bool shouldEcho(std::string value, int timestamp);
    bool isEchoValid(std::vector<std::pair<int, std::string>> echoes, std::string value, int timestamp, std::string type);
}
#endif /* server_hpp */
