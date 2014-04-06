package org.sql2o.converters;

import java.util.UUID;

/**
 * @author aldenquimby@gmail.com
 * @since 4/6/14
 */
public class UUIDWrapper
{
    private UUID text;

    public UUIDWrapper() {
    }

    public UUIDWrapper(UUID text) {
        this.text = text;
    }

    public UUID getText() {
        return text;
    }

    public void setText(UUID text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UUIDWrapper that = (UUIDWrapper) o;

        if (text != null ? !text.equals(that.text) : that.text != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }
}
