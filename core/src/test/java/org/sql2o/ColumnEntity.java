package org.sql2o;

import javax.persistence.Column;

public class ColumnEntity {

    private int id;
    @Column(name = "text_col")
    private String text;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
