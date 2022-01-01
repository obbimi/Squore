package com.doubleyellow.view;

import com.doubleyellow.util.Direction;

public interface DrawArrows {
    void hideArrows();
    void drawArrow(int[] vRelatedGuiElements, int bgColor);
    void drawArrow(int[] vRelatedGuiElements, Direction[] arrowDirection, int bgColor);
}
