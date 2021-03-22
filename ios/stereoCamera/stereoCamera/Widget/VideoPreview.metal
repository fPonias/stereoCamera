//
//  VideoPreview.metal
//  stereoCamera
//
//  Created by Cody Munger on 3/16/21.
//  Copyright © 2021 cody. All rights reserved.
//

#include <metal_stdlib>
using namespace metal;

typedef struct{
    float4 renderedCoordinate [[position]];
    float2 textureCoordinate;
} TextureMappingVertex;

vertex TextureMappingVertex mapTexture(const device TextureMappingVertex *vertices [[buffer(0)]],
                                       uint vertex_id  [[vertex_id]]) {
    return vertices[vertex_id];
}


fragment half4 displayTexture(TextureMappingVertex mappingVertex [[ stage_in ]],
                              texture2d<float, access::sample> texture [[ texture(0) ]]) {
    constexpr sampler s(address::clamp_to_edge, filter::linear);
    return half4(texture.sample(s, mappingVertex.textureCoordinate));
}

//expect histArray to be an int[512] //9 bits
kernel void
histogram(texture2d<float, access::read> texture [[ texture(0) ]],
          device atomic_int* histArray [[ buffer(0)]],
          device uint* marginArray [[ buffer(1) ]],
          uint2 gid [[ thread_position_in_grid ]] ) {
    uint w = texture.get_width();
    uint h = texture.get_height();
    
    if (gid[0] < marginArray[0] || gid[0] > w - marginArray[2])
        return;
    
    if (gid[1] < marginArray[1] || gid[1] > h - marginArray[3])
        return;
    
    float4 val = texture.read(gid);
    
    int idx = ((int) (val[0] * 8)) << 6;
    idx |= ((int) (val[1] * 8)) << 3;
    idx |= (int) (val[2] * 8);
    
    atomic_fetch_add_explicit(histArray + idx, 1, memory_order_relaxed);
}
