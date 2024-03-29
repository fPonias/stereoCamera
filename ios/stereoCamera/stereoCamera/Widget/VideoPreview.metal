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
                                       constant float4x4 &transform [[ buffer(1) ]],
                                       uint vertex_id  [[vertex_id]]) {
    float4 pixel = vertices[vertex_id].renderedCoordinate;
    TextureMappingVertex ret;
    ret.renderedCoordinate = transform*pixel;
    ret.textureCoordinate[0] = vertices[vertex_id].textureCoordinate[0];
    ret.textureCoordinate[1] = vertices[vertex_id].textureCoordinate[1];
    
    return ret;
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
    
    if (gid[0] < marginArray[0] || gid[0] >= w - marginArray[2])
        return;
    
    if (gid[1] < marginArray[1] || gid[1] >= h - marginArray[3])
        return;
    
    float4 val = texture.read(gid);
    float avg = (val[0] + val[1] + val[2]) / 3.0f;
    int idx = 256 * avg;
    
    atomic_fetch_add_explicit(histArray + idx, 1, memory_order_relaxed);
}

typedef struct{
    float4 margins;
    float2 pixelDims;
} marginStr;

kernel void
histogramReduced(texture2d<float, access::sample> texture [[ texture(0) ]],
                 device atomic_int* histArray [[ buffer(0) ]],
                 device float* margins [[ buffer(1) ]],
                 uint2 gid [[ thread_position_in_grid ]]) {
    uint left = margins[0] * texture.get_width();
    uint right = margins[2] * texture.get_width();
    uint top = margins[1] * texture.get_height();
    uint bottom = margins[3] * texture.get_height();
    
    if (gid[0] < left || gid[0] >= right)
        return;
    
    if (gid[1] < top || gid[1] >= bottom)
        return;
    
    float2 c;
    c[0] = (float)(gid[0] - left) * margins[4];
    c[1] = (float)(gid[1] - top) * margins[5];
    
    constexpr sampler s(address::clamp_to_edge, filter::linear);
    float4 val = float4(texture.sample(s, c));
    float avg = (val[0] + val[1] + val[2]) / 3.0f;
    uint idx = 256 * avg;
        
    atomic_fetch_add_explicit(histArray + idx, 1, memory_order_relaxed);
}

kernel void
autoContrast(texture2d<float, access::read_write> texture [[texture(0)]],
             device float* values [[ buffer(0)]],
             uint2 gid [[ thread_position_in_grid ]]) {
    float4 val = texture.read(gid);
    for (int i = 0; i < 3; i++) {
        val[i] = values[0] * val[i] + values[1];
    }
    
    texture.write(val, gid);
}

uint2 crop_rotate(uint2 dest, uint2 dims, uint rot) //why are the images flipped at this point?
{
    uint2 ret;
    switch (rot) {
        case 0:
            ret[0] = dims[0] - dest[0];
            ret[1] = dest[1];
            return ret;
        case 1:
            ret[1] = dims[0] - dest[0];
            ret[0] = dest[1];
            return ret;
        case 2:
            ret[0] = dest[0];
            ret[1] = dims[1] - dest[1];
            return ret;
        default:
            ret[0] = dims[1] - dest[1];
            ret[1] = dims[0] - dest[0];
            return ret;
    }
}

kernel void
clear(texture2d<float, access::write> inTexture [[ texture(0) ]],
      uint2 gid [[ thread_position_in_grid ]]) {
    float4 pix;
    pix[0] = 0.0;
    pix[1] = 0.0;
    pix[2] = 0.0;
    pix[3] = 1.0;
    
    inTexture.write(pix, gid);
}

kernel void
crop(texture2d<float, access::read> inTexture [[ texture(0) ]],
          texture2d<float, access::write> outTexture [[ texture(1) ]],
          device uint* offset [[ buffer(0)]],
          uint2 gid [[ thread_position_in_grid ]]) {
    uint2 dims;
    dims[0] = outTexture.get_width();
    dims[1] = outTexture.get_height();
    
    if (gid[0] < offset[0] || gid[0] > offset[0] + dims[0])
        return;
        
    if (gid[1] < offset[1] || gid[1] > offset[1] + dims[1])
        return;
    
    uint2 dest;
    dest[0] = gid[0] - offset[0];
    dest[1] = gid[1] - offset[1];
    dest = crop_rotate(dest, dims, offset[2]);
    
    float4 pix = inTexture.read(gid);
    outTexture.write(pix, dest);
}

kernel void
colorMask(texture2d<float, access::sample> inTexture [[ texture(0) ]],
          texture2d<float, access::read_write> outTexture [[ texture(1) ]],
          device float* mask [[ buffer(0)]],
          device uint2* offset [[ buffer(1) ]],
          device float3x3* transform [[ buffer(2) ]],
          uint2 gid [[ thread_position_in_grid ]]) {
    uint2 sz = offset[1];
    
    if (gid[0] > sz[0] || gid[1] > sz[1])
        return;
    
    float3 texCoord;
    if (gid[0] <= sz[0])
        texCoord[0] = (float) gid[0] / (float) sz[0];
    else
        texCoord[0] = (float) sz[0] / (float) gid[0];
    
    if (gid[1] <= sz[1])
        texCoord[1] = (float) gid[1] / (float) sz[1];
    else
        texCoord[1] = (float) sz[1] / (float) gid[1];
    
    texCoord[2] = 1.0f;
    
    texCoord += float3 {-0.5f, -0.5f, 0.0f};
    texCoord = *transform * texCoord;
    texCoord += float3 {0.5f, 0.5f, 0.0f};
    
    constexpr sampler s(address::clamp_to_edge, filter::nearest);
    float4 val = inTexture.sample(s, float2 {texCoord[0], texCoord[1]});
    gid += offset[0];
    float4 cur = outTexture.read(gid);
    
    for (int i = 0; i < 3; i++)
    {
        val[i] = min(1.0, val[i] * mask[i] + cur[i]);
    }
    
    outTexture.write(val, gid);
}

kernel void
difference(texture2d<float, access::read> textureA [[ texture(0) ]],
          texture2d<float, access::read> textureB [[ texture(1) ]],
          device atomic_int* diff [[ buffer(0) ]],
          device int* args [[ buffer(1)]],
          uint2 gid [[ thread_position_in_grid ]]) {
    int spacing = args[0];
    int magnitude = args[1];
    int width = textureA.get_width();
    int height = textureA.get_height();
    
    uint cols = width / spacing;
    uint rows = height / spacing;
    
    if (gid[0] >= cols || gid[1] >= rows)
    {
        return;
    }
    
    int cOffset = gid[0] * spacing;
    int rOffset = gid[1] * spacing;
    float aTotal = 0.0;
    float bTotal = 0.0;
    for (int r = rOffset; r < rOffset + spacing; r++)
    {
        for (int c = cOffset; c < cOffset + spacing; c++)
        {
            uint2 crd; crd[0] = c; crd[1] = r;
            float4 aval = textureA.read(crd);
            float4 bval = textureB.read(crd);
            
            aTotal += (aval[0] + aval[1] + aval[2]) / 3.0f;
            bTotal += (bval[0] + bval[1] + bval[2]) / 3.0f;
        }
    }
    
    float area = (float) (spacing * spacing);
    aTotal /= area;
    bTotal /= area;
    int diffAdd = abs(bTotal - aTotal) * magnitude;
    
    atomic_fetch_add_explicit(diff, diffAdd, memory_order_relaxed);
}
