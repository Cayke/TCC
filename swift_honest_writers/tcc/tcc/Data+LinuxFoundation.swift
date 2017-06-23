//
//  Data+LinuxFoundation.swift
//  tcc
//
//  Created by Cayke Prudente on 22/06/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

extension Data {
    func bridge() -> NSData {
    	#if os(Linux)
    		return NSData(base64Encoded: self, options: .init(rawValue: 0))!
    	#else
        	return self as! NSData
        #endif
    }
}
