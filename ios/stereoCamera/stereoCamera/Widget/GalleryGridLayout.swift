//
//  GalleryGridLayout.swift
//  stereoCamera
//
//  Created by hallmarklabs on 6/22/18.
//  Copyright © 2018 cody. All rights reserved.
//

import UIKit

class GalleryGridLayout : UICollectionViewFlowLayout
{
    public func getItemsPerRow() -> Int
    {
        var contentByItem:ldiv_t
        let contentSize = collectionViewContentSize
        
        if (scrollDirection == UICollectionViewScrollDirection.vertical)
        {
            contentByItem = ldiv(Int(contentSize.width), Int(itemSize.width));
        }
        else
        {
            contentByItem = ldiv (Int(contentSize.height), Int(itemSize.height));
        }
        
        return contentByItem.quot
    }
    
    override func prepare()
    {
        super.prepare()
        
        var contentByItem:ldiv_t
        let contentSize = collectionViewContentSize
        if (scrollDirection == UICollectionViewScrollDirection.vertical)
        {
            contentByItem = ldiv(Int(contentSize.width), Int(itemSize.width));
        }
        else
        {
            contentByItem = ldiv (Int(contentSize.height), Int(itemSize.height));
        }
        
        let layoutSpacingVal = Float(contentByItem.rem) / Float(contentByItem.quot + 1)
        let layoutSpacingValue = CGFloat(Int(layoutSpacingVal))
        
        let origMinLineSpacing = minimumLineSpacing
        let origMinInterItemSpacing = minimumInteritemSpacing
        let origSectionInset = sectionInset
        
        if (layoutSpacingValue != origMinLineSpacing || layoutSpacingValue != origMinInterItemSpacing || layoutSpacingValue != origSectionInset.left || layoutSpacingValue != origSectionInset.right || layoutSpacingValue != origSectionInset.top || layoutSpacingValue != origSectionInset.bottom)
        {
            let insetForItem = UIEdgeInsets(top: layoutSpacingValue, left: layoutSpacingValue, bottom: layoutSpacingValue, right: layoutSpacingValue)
            
            minimumLineSpacing = layoutSpacingValue
            minimumInteritemSpacing = layoutSpacingValue
            sectionInset = insetForItem
        }
    }
}
