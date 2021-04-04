//
//  MulticastDelegate.swift
//  stereoCamera
//
//  Created by Cody Munger on 4/3/21.
//  Copyright © 2021 cody. All rights reserved.
//

import Foundation

class MulticastDelegate<T> {

    private let delegates: NSHashTable<AnyObject> = NSHashTable.weakObjects()

    func add(_ delegate: T) {
        delegates.add(delegate as AnyObject)
    }

    func remove(_ delegateToRemove: T) {
        for delegate in delegates.allObjects.reversed() {
            if delegate === delegateToRemove as AnyObject {
                delegates.remove(delegate)
            }
        }
    }

    func invoke(_ invocation: (T) -> Void) {
        for delegate in delegates.allObjects.reversed() {
            invocation(delegate as! T)
        }
    }
}
