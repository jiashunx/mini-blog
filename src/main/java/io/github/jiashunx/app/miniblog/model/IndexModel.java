package io.github.jiashunx.app.miniblog.model;

import java.util.List;

/**
 * @author jiashunx
 */
public class IndexModel {

    private boolean prevEnabled;
    private List<PageableIndex> pageableIndexList;
    private boolean nextEnabled;

    public boolean isPrevEnabled() {
        return prevEnabled;
    }

    public void setPrevEnabled(boolean prevEnabled) {
        this.prevEnabled = prevEnabled;
    }

    public List<PageableIndex> getPageableIndexList() {
        return pageableIndexList;
    }

    public void setPageableIndexList(List<PageableIndex> pageableIndexList) {
        this.pageableIndexList = pageableIndexList;
    }

    public boolean isNextEnabled() {
        return nextEnabled;
    }

    public void setNextEnabled(boolean nextEnabled) {
        this.nextEnabled = nextEnabled;
    }
}
