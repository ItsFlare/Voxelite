vec2 clampRay(vec2 origin, vec2 target) {
    //Assumes origin is within bounds

    vec2 targetNDC = abs(target * 2 - 1);

    float scale = 1;
    if(targetNDC.x > 1 || targetNDC.y > 1) {
        //Abs transforms to first quadrant
        vec2 originNDC = abs(origin * 2 - 1);

        vec2 dirNDC = targetNDC - originNDC;

        if(dirNDC.x > 0) {
            scale = min(scale, (1 - originNDC.x) / dirNDC.x);
        }

        if(dirNDC.y > 0) {
            scale = min(scale, (1 - originNDC.y) / dirNDC.y);
        }
    }

    return (target - origin) * scale;
}