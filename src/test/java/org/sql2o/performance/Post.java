package org.sql2o.performance;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

/**
 * @author aldenquimby@gmail.com
 */
@Entity
public class Post
{
    @Id @GeneratedValue
    public int id;
    private String text;
    private Date creationDate;
    private Date lastChangeDate;
    private Integer counter1;
    private Integer counter2;
    private Integer counter3;
    private Integer counter4;
    private Integer counter5;
    private Integer counter6;
    private Integer counter7;
    private Integer counter8;
    private Integer counter9;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public void setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
    }

    public Date getLastChangeDate()
    {
        return lastChangeDate;
    }

    public void setLastChangeDate(Date lastChangeDate)
    {
        this.lastChangeDate = lastChangeDate;
    }

    public Integer getCounter1()
    {
        return counter1;
    }

    public void setCounter1(Integer counter1)
    {
        this.counter1 = counter1;
    }

    public Integer getCounter2()
    {
        return counter2;
    }

    public void setCounter2(Integer counter2)
    {
        this.counter2 = counter2;
    }

    public Integer getCounter3()
    {
        return counter3;
    }

    public void setCounter3(Integer counter3)
    {
        this.counter3 = counter3;
    }

    public Integer getCounter4()
    {
        return counter4;
    }

    public void setCounter4(Integer counter4)
    {
        this.counter4 = counter4;
    }

    public Integer getCounter5()
    {
        return counter5;
    }

    public void setCounter5(Integer counter5)
    {
        this.counter5 = counter5;
    }

    public Integer getCounter6()
    {
        return counter6;
    }

    public void setCounter6(Integer counter6)
    {
        this.counter6 = counter6;
    }

    public Integer getCounter7()
    {
        return counter7;
    }

    public void setCounter7(Integer counter7)
    {
        this.counter7 = counter7;
    }

    public Integer getCounter8()
    {
        return counter8;
    }

    public void setCounter8(Integer counter8)
    {
        this.counter8 = counter8;
    }

    public Integer getCounter9()
    {
        return counter9;
    }

    public void setCounter9(Integer counter9)
    {
        this.counter9 = counter9;
    }
}
