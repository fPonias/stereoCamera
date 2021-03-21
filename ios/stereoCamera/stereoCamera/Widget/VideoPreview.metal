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

vertex TextureMappingVertex mapTexture(device TextureMappingVertex *vertices [[buffer(0)]],
                                       uint vertex_id  [[vertex_id]]) {
    return vertices[vertex_id];
}


fragment half4 displayTexture(TextureMappingVertex mappingVertex [[ stage_in ]],
                              texture2d<float, access::sample> texture [[ texture(0) ]]) {
    constexpr sampler s(address::clamp_to_edge, filter::linear);
    return half4(texture.sample(s, mappingVertex.textureCoordinate));
}
