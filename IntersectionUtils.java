public class IntersectionUtils {
    //Takes a (l,t,r,b) rect and reduces/increases the values so that they become a boundary rect for all pixels with height >= threshold
    public static boolean boundary2DCutThresholded(int threshold, int[] boundary, ZDraw draw) {
        return boundary2DCutThresholded(threshold, boundary, draw.start, draw.stride, draw.data);
    }
    public static boolean boundary2DCutThresholded(int threshold, int[] boundary, int start, int stride, int[] data) {
        int l = boundary[0], t = boundary[1], r = boundary[2], b = boundary[3];

        int outL = r + 1, outT = b + 1, outR = l, outB = t;
        for (int y=t;y<b;y++){
            int base = (start + (y*stride));
            for (int x=l;x<r;x++)
                if (data[base+x] >= threshold) {
                    if (y < outT) outT = y;
                    if (y >= outB) outB = y + 1;
                    if (x < outL) outL = x;
                    if (x >= outR) outR = x + 1;
                }
        }
        boundary[0] = outL;
        boundary[1] = outT;
        boundary[2] = outR;
        boundary[3] = outB;
        return (outL < outR) && (outT < outB);
    }
    public static void boundary2DShift(int [] boundary, ScenePrimitive from, ScenePrimitive to) {
        Vector3i fromVec = from.centerI();
        Vector3i toVec = to.centerI();
        int dx = toVec.x - fromVec.x;
        int dy = toVec.y - fromVec.y;
        boundary[0] += dx;
        boundary[1] += dy;
        boundary[2] += dx;
        boundary[3] += dy;
    }
}
