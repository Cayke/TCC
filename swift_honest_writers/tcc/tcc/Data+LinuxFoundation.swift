//
//  Data+LinuxFoundation.swift
//  tcc
//
//  Created by Cayke Prudente on 22/06/17.
//  Copyright © 2017 Cayke Prudente. All rights reserved.
//

import Foundation

extension Data {
#if !os(Linux)
    func bridge() -> NSData {
        return self as NSData
    }
#endif
}
