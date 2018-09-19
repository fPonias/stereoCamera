//
//  AppDelegate.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/12/18.
//  Copyright © 2018 cody. All rights reserved.
//

import UIKit

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    var window: UIWindow?

    private static var _instance:AppDelegate?
    public static var instance:AppDelegate?
    {
        get { return _instance }
    }

    override init()
    {
        super.init()
        AppDelegate._instance = self
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplicationLaunchOptionsKey: Any]?) -> Bool
    {
        let sz = listenerCount()
        
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.application?(application, didFinishLaunchingWithOptions: launchOptions)
        }}
    
        return true
    }

    func applicationWillResignActive(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationWillResignActive?(application)
        }}
    }

    func applicationDidEnterBackground(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationDidEnterBackground?(application)
        }}
    }

    func applicationWillEnterForeground(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationWillEnterForeground?(application)
        }}
    }

    func applicationDidBecomeActive(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationDidBecomeActive?(application)
        }}
    }

    func applicationWillTerminate(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationWillTerminate?(application)
        }}
    }

    func applicationDidFinishLaunching(_ application: UIApplication)
    {
        let sz = listenerCount()
        if (sz > 0) {for i in 0 ... (sz - 1)
        {
            let listener = getListener(at: i)
            _ = listener?.applicationDidFinishLaunching?(application)
        }}
    }
    
    private var listeners = NSPointerArray.weakObjects()
    
    func addListener(_ listener:UIApplicationDelegate)
    {
        let ptr = Unmanaged.passUnretained(listener).toOpaque()
        listeners.addPointer(ptr)
    }
    
    func listenerCount() -> Int
    {
        return listeners.count
    }
    
    func getListener(at:Int) -> UIApplicationDelegate?
    {
        guard (at < listeners.count) else { return nil }
        let ptr = listeners.pointer(at: at)
        guard (ptr != nil) else { return nil }
        
        return Unmanaged<AppDelegate>.fromOpaque(ptr!).takeUnretainedValue()
    }
}

