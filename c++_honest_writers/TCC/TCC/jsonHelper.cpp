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

int getIntWithValueFromDocument(rapidjson::Document *document, std::string value)
{
    Value::MemberIterator hello = document->FindMember(value.c_str());
    assert(hello != document->MemberEnd());
    assert(hello->value.IsInt());
    return hello->value.GetInt();
}

std::string getStringWithValueFromDocument(rapidjson::Document *document, std::string value)
{
    Value::MemberIterator hello = document->FindMember(value.c_str());
    assert(hello != document->MemberEnd());
    assert(hello->value.IsString());
    return hello->value.GetString();
}

std::string getJSONStringForDocument(rapidjson::Document *document)
{
    StringBuffer sb;
    PrettyWriter<StringBuffer> writer(sb);
    document->Accept(writer);
    return sb.GetString();
}

void addValueToDocument(rapidjson::Document *document, std::string key, int value)
{
    rapidjson::Value vKey;
    vKey.SetString(StringRef(key.c_str()));
    
    rapidjson::Value vValue;
    vValue.SetInt(value);
    document->AddMember(vKey, vValue, document->GetAllocator());
}

void addValueToDocument(rapidjson::Document *document, std::string key, std::string value)
{
    rapidjson::Value vKey;
    vKey.SetString(StringRef(key.c_str()));
    
    rapidjson::Value vValue;
    vValue.SetString(StringRef(value.c_str()));
    
    document->AddMember(vKey, vValue, document->GetAllocator());
}



