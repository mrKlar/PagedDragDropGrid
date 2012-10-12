package ca.laplanete.mobile.pageddragdropgrid;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import ca.laplanete.mobile.example.ExamplePagedDragDropGridAdapter;

public class PagedDragDropGrid extends HorizontalScrollView implements PagedContainer {

    private int mActivePage = 0;
	private DragDropGrid grid;
	private PagedDragDropGridAdapter adapter;
    
    public PagedDragDropGrid(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context) {
        super(context);
        initPagedScroll();
        initGrid();
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs, int defStyle, PagedDragDropGridAdapter adapter) {
        super(context, attrs, defStyle);
        this.adapter = adapter;
        initPagedScroll();
        initGrid();   
    }
 
    public PagedDragDropGrid(Context context, AttributeSet attrs, PagedDragDropGridAdapter adapter) {
        super(context, attrs);
        this.adapter = adapter;
        initPagedScroll();
        initGrid();   
    }
 
    public PagedDragDropGrid(Context context, PagedDragDropGridAdapter adapter) {
        super(context);
        this.adapter = adapter;
        initPagedScroll();        
        initGrid();    	
    }

	private void initGrid() {
		grid = new DragDropGrid(getContext());
    	addView(grid);
	}
 
    public void initPagedScroll(){
        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL ){
                    int scrollX = getScrollX();                    
                    int onePageWidth = v.getMeasuredWidth();
                    int page = ((scrollX + (onePageWidth/2))/onePageWidth);
                    scrollToPage(page);
                    return true;
                } else{
                    return false;
                }
            }
        });
    }
    
    public void setAdapter(ExamplePagedDragDropGridAdapter adapter) {
    	this.adapter = adapter;
		grid.setAdapter(adapter);
		grid.setContainer(this);
	}

	@Override
	public void scrollToPage(int page) {
		mActivePage = page;
		int onePageWidth = getMeasuredWidth();
		int scrollTo = page*onePageWidth;
        smoothScrollTo(scrollTo, 0);
	}

	@Override
	public void scrollLeft() {		
		int newPage = mActivePage-1;
		if (canScrollLeft()) {
			scrollToPage(newPage);
		}
	}

	@Override
	public void scrollRight() {
		int newPage = mActivePage+1;
		if (canScrollRight()) {		
			scrollToPage(newPage);
		}
	}

	@Override
	public int currentPage() {
		return mActivePage;
	}

	@Override
	public void enableScroll() {
		requestDisallowInterceptTouchEvent(false);
	}

	@Override
	public void disableScroll() {
		requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public boolean canScrollRight() {
		int newPage = mActivePage+1;
		return (newPage < adapter.pageCount());
	}

	@Override
	public boolean canScrollLeft() {
		int newPage = mActivePage-1;
		return (newPage >= 0);
	}	
}
