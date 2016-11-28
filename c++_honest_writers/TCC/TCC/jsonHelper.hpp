//
//  jsonHelper.hpp
//  TCC
//
//  Created by Cayke Prudente on 25/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#ifndef jsonHelper_hpp
#define jsonHelper_hpp

#include "rapidjson/document.h"
#include "rapidjson/writer.h"
#include "rapidjson/stringbuffer.h"

rapidjson::Document parseJsonStringToDocument (std::string jsonString);
int getIntWithValueFromDocument(rapidjson::Document *document, std::string value);
std::string getStringWithValueFromDocument(rapidjson::Document *document, std::string value);

#endif /* jsonHelper_hpp */