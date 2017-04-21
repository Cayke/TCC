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
int getIntWithKeyFromDocument(rapidjson::Document *document, std::string key);
std::string getStringWithKeyFromDocument(rapidjson::Document *document, std::string key);
std::string getJSONStringForDocument(rapidjson::Document *document);
std::vector<std::pair<int, std::string>> getEchoesArrayWithKeyFromDocument(rapidjson::Document *document, std::string key);
void addValueToDocument(rapidjson::Document *document, std::string key, int value);
void addValueToDocument(rapidjson::Document *document, std::string key, std::string value);
void addValueToValueStruct(rapidjson::Value *valueObject, rapidjson::Document *document, std::string key, int value);
void addValueToValueStruct(rapidjson::Value *valueObject, rapidjson::Document *document, std::string key, std::string value);
void addValueToDocument(rapidjson::Document *document, std::string key, rapidjson::Value *valueObject);


#endif /* jsonHelper_hpp */
