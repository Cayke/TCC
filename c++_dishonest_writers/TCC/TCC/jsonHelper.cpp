//
//  jsonHelper.cpp
//  TCC
//
//  Created by Cayke Prudente on 25/11/16.
//  Copyright © 2016 Cayke Prudente. All rights reserved.
//

#include "jsonHelper.hpp"
#include "rapidjson/prettywriter.h"

#include <stdio.h>
#include <string>

using namespace std;
using namespace rapidjson;

Document parseJsonStringToDocument (std::string jsonString)
{
    Document doc;
    doc.Parse(jsonString.c_str());
    
    if (doc.HasParseError())
        exit(-1);
    else
        return doc;
}

int getIntWithKeyFromDocument(rapidjson::Document *document, std::string key)
{
    Value::MemberIterator value = document->FindMember(key.c_str());
    assert(value != document->MemberEnd());
    assert(value->value.IsInt());
    return value->value.GetInt();
}

std::string getStringWithKeyFromDocument(rapidjson::Document *document, std::string key)
{
    Value::MemberIterator value = document->FindMember(key.c_str());
    assert(value != document->MemberEnd());
    assert(value->value.IsString());
    return value->value.GetString();
}

std::string getJSONStringForDocument(rapidjson::Document *document)
{
    StringBuffer sb;
    //PrettyWriter<StringBuffer> writer(sb);
    Writer<StringBuffer, Document::EncodingType, ASCII<> > writer(sb);
    document->Accept(writer);
    return sb.GetString();
}

void addValueToDocument(rapidjson::Document *document, std::string key, int value)
{
    rapidjson::Value vKey;
    //vKey.SetString(StringRef(key.c_str()));
    vKey.SetString(key.c_str(), (int) key.length(), document->GetAllocator());
    
    rapidjson::Value vValue;
    vValue.SetInt(value);
    document->AddMember(vKey, vValue, document->GetAllocator());
}

void addValueToDocument(rapidjson::Document *document, std::string key, std::string value)
{
    rapidjson::Value vKey;
    //vKey.SetString(StringRef(key.c_str()));
    vKey.SetString(key.c_str(), (int) key.length(), document->GetAllocator());
    
    rapidjson::Value vValue;
    //vValue.SetString(StringRef(value.c_str()));
    vValue.SetString(value.c_str(), (int) value.length(), document->GetAllocator());
    
    document->AddMember(vKey, vValue, document->GetAllocator());
}

void addValueToValueStruct(rapidjson::Value *valueObject, rapidjson::Document *document, std::string key, int value)
{
    rapidjson::Value vKey;
    //vKey.SetString(StringRef(key.c_str()));
    vKey.SetString(key.c_str(), (int) key.length(), document->GetAllocator());
    
    rapidjson::Value vValue;
    vValue.SetInt(value);
    
    valueObject->AddMember(vKey, vValue, document->GetAllocator());
}
void addValueToValueStruct(rapidjson::Value *valueObject, rapidjson::Document *document, std::string key, std::string value)
{
    rapidjson::Value vKey;
    //vKey.SetString(StringRef(key.c_str()));
    vKey.SetString(key.c_str(), (int) key.length(), document->GetAllocator());
    
    rapidjson::Value vValue;
    //vValue.SetString(StringRef(value.c_str()));
    vValue.SetString(value.c_str(), (int) value.length(), document->GetAllocator());
    
    valueObject->AddMember(vKey, vValue, document->GetAllocator());
}

void addValueToDocument(rapidjson::Document *document, std::string key, rapidjson::Value *valueObject)
{
    rapidjson::Value vKey;
    //vKey.SetString(StringRef(key.c_str()));
    vKey.SetString(key.c_str(), (int) key.length(), document->GetAllocator());
    
    document->AddMember(vKey, *valueObject, document->GetAllocator());
}


