package com.example.handlingformsubmission;

public class Greeting {

    private long id;
    private String content;

    /* example */
    public SomeNestedPOJO someNestedPOJO;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /* example */
    public SomeNestedPOJO getSomeNestedPOJO() {
        return someNestedPOJO;
    }
    public void setSomeNestedPOJO(SomeNestedPOJO someNestedPOJO) {
        this.someNestedPOJO = someNestedPOJO;
    }
}