package id.skripsi.fariz.mobilevisonapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by didi on 5/13/2017.
 */

public class TextModel {

    String text;
    List<String> zoom = new ArrayList<>();
    int i;

    public List<String> getZoom() {
        return zoom;
    }

    public void setZoom(List<String> zoom) {
        this.zoom = zoom;
    }

    public TextModel() {

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
