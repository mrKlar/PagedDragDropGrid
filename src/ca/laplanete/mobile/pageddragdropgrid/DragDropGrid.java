package ca.laplanete.mobile.pageddragdropgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

public class DragDropGrid extends ViewGroup implements OnTouchListener, OnLongClickListener {

	public static int ANIMATION_DURATION = 250;
	private static int EGDE_DETECTION_MARGIN = 35;

	private PagedDragDropGridAdapter adapter;
	private PagedContainer container;
	
	private SparseArray<Integer> newPositions = new SparseArray<Integer>();

	private int gridPageWidth = 0;
	private int dragged = -1;
	public int currentTarget = -1;
	private int columnWidthSize;
	private int rowHeightSize;
	private int biggestChildWidth;
	private int biggestChildHeight;
	private int computedColumnCount;
	private int computedRowCount;
	private float initialX;
	private float initialY;
	private boolean movingView;
	private int lastTarget = -1;
	private boolean isOnEdge = false;
	private Timer edgeScrollTimer;
	
	final Handler edgeTimerHandler = new Handler();
	private int lastTouchX;
	private int lastTouchY;

	public DragDropGrid(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public DragDropGrid(Context context) {
		super(context);
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs, int defStyle, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context, attrs, defStyle);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	public DragDropGrid(Context context, AttributeSet attrs, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context, attrs);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	public DragDropGrid(Context context, PagedDragDropGridAdapter adapter, PagedContainer container) {
		super(context);
		this.adapter = adapter;
		this.container = container;
		init();
	}

	private void init() {
		setOnTouchListener(this);
		setOnLongClickListener(this);
	}

	public void setAdapter(PagedDragDropGridAdapter adapter) {
		this.adapter = adapter;
		addChildViews();
	}

	private void addChildViews() {
		for (int page = 0; page < adapter.pageCount(); page++) {
			for (int item = 0; item < adapter.itemCountInPage(page); item++) {
				addView(adapter.view(page, item));
			}
		}
	}

	private void animateMoveAllItems() {
		Animation rotateAnimation = createFastRotateAnimation();

		for (int i=0; i< getChildCount(); i++) {
			View child = getChildAt(i);
			child.startAnimation(rotateAnimation);
		 }
	}
	
	private void cancelAnimations() {
		 for (int i=0; i < getChildCount(); i++) {
			 View child = getChildAt(i);
			 child.clearAnimation();
		 }
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			touchDown(event);
			break;
		case MotionEvent.ACTION_MOVE:
			touchMove(event);
			break;
		case MotionEvent.ACTION_UP:
			touchUp();
			break;
		}
		if (dragged != -1)
			return true;
		return false;
	}

	private void touchUp() {
		cancelEdgeTimer();
		reorderChildren();
		movingView = false;
		dragged = -1;
		lastTarget = -1;
		container.enableScroll();		
		cancelAnimations();
	}

	private void touchDown(MotionEvent event) {
		initialX = event.getRawX();
		initialY = event.getRawY();
	}

	private void touchMove(MotionEvent event) {
		if (movingView && dragged != -1) {
			lastTouchX = (int) event.getX();
			lastTouchY = (int) event.getY();
			moveDraggedView(lastTouchX, lastTouchY);
			manageSwapPosition(lastTouchX, lastTouchY);
			manageEdgeCoordinates(lastTouchX);
		}
	}

	private void moveDraggedView(int x, int y) {
		View childAt = getChildAt(dragged);
		int width = childAt.getMeasuredWidth();
		int height = childAt.getMeasuredHeight();

		int l = x - (1 * width / 2);
		int t = y - (1 * height / 2);

		childAt.layout(l, t, l + width, t + height);
	}

	private void manageSwapPosition(int x, int y) {
		int target = getTargetFromCoor(x, y);
		if (childHasMoved(target) && target != lastTarget) {
			animateGap(target);										
			lastTarget = target;
		}
	}

	private void manageEdgeCoordinates(int x) {
		final boolean onRightEdge = onRightEdgeOfScreen(x);
		final boolean onLeftEdge = onLeftEdgeOfScreen(x);
		
		if (onRightEdge || onLeftEdge) {			
			if (!isOnEdge) {
				
				startEdgeDelayTimer(onRightEdge, onLeftEdge);	
				isOnEdge = true;											
			}
		} else {
			stopAnimateOnTheEdge();
			isOnEdge = false;
			cancelEdgeTimer();
		}
	}

	private void stopAnimateOnTheEdge() {
		View draggedView = getChildAt(dragged);
		draggedView.clearAnimation();
	}

	private void cancelEdgeTimer() {
		
		if (edgeScrollTimer != null) {
			Log.d("Timer", "cancel");
			edgeScrollTimer.cancel();
			edgeScrollTimer = null;
		}
	}

	private void startEdgeDelayTimer(final boolean onRightEdge, final boolean onLeftEdge) {
		if (canScrollEitherSide(onRightEdge, onLeftEdge)) {
			animateOnTheEdge();
			if (edgeScrollTimer == null) {
				edgeScrollTimer = new Timer();
				edgeScrollTimer.schedule(new TimerTask() {          
				    @Override
				    public void run() {
				    	if (isOnEdge) {
				    		isOnEdge = false;
				    		edgeTimerHandler.post(new Runnable() {	
								@Override
								public void run() {
									scroll(onRightEdge, onLeftEdge);
									cancelAnimations();
									animateMoveAllItems();
								}
							});
				    	} 
				    }
				}, 1000);
			}
		}
	}

	private boolean canScrollEitherSide(final boolean onRightEdge, final boolean onLeftEdge) {
		return (onLeftEdge && container.canScrollLeft()) || (onRightEdge && container.canScrollRight());
	}

	private void scroll(boolean onRightEdge, boolean onLeftEdge) {	
		cancelEdgeTimer();
		
		if (onLeftEdge && container.canScrollLeft()) {

			moveItemToPageOnLeft(dragged);	
			moveDraggedToPageOnLeft();
									
			container.scrollLeft();
			int currentPage = getCurrentPage();
			int lastItem = adapter.itemCountInPage(currentPage)-1;
			dragged = absolutePositionOfItemLocatedInPage(currentPage, lastItem);

			onLayout(true, getLeft(), getTop(), getRight(), getBottom());
			stopAnimateOnTheEdge();

		} else if (onRightEdge && container.canScrollRight()) {
			moveItemToPageOnRight(dragged);
			moveDraggedToPageOnRight();
			
			container.scrollRight();
			int currentPage = getCurrentPage();
			int lastItem = adapter.itemCountInPage(currentPage)-1;
			dragged = absolutePositionOfItemLocatedInPage(currentPage, lastItem);

			onLayout(true, getLeft(), getTop(), getRight(), getBottom());
			stopAnimateOnTheEdge();
		}
		isOnEdge = false;	
	}

	
	private void moveDraggedToPageOnLeft() {
		List<View> children = saveChildren();
		removeAllViews();

		List<View> reorderedViews = reeorderView(children);
		int draggedEndPosition = newPositions.get(dragged, dragged);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);
		
		int currentPage = getCurrentPage();
		int indexFirstElementInCurrentPage = 0;
		for (int i=0;i<currentPage;i++) {
			indexFirstElementInCurrentPage += adapter.itemCountInPage(i);
		}

		int indexOfDraggedOnNewPage = indexFirstElementInCurrentPage-1;
		
		reorderAndAddViews(reorderedViews, draggedView, indexOfDraggedOnNewPage);
	}

	private void moveDraggedToPageOnRight() {
		List<View> children = saveChildren();
		removeAllViews();

		List<View> reorderedViews = reeorderView(children);
		int draggedEndPosition = newPositions.get(dragged, dragged);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);
		
		int currentPage = getCurrentPage();
		int indexLastElementInNextPage = 0;
		for (int i=0;i<=currentPage+1;i++) {
			indexLastElementInNextPage += adapter.itemCountInPage(i);
		}

		int indexOfDraggedOnNewPage = indexLastElementInNextPage-1;
		reorderAndAddViews(reorderedViews, draggedView, indexOfDraggedOnNewPage);	
	}
	
	private void reorderAndAddViews(List<View> reorderedViews, View draggedView, int indexOfDraggedOnNewPage) {
		reorderedViews.add(indexOfDraggedOnNewPage,draggedView);
		
		newPositions.clear();
		
		for (View view : reorderedViews) {
			addView(view);
		}
	}

	private boolean onLeftEdgeOfScreen(int x) {
		int currentPage = container.currentPage();
		
		int leftEdgeXCoor = currentPage*gridPageWidth;
		int distanceFromEdge = x - leftEdgeXCoor;
		return (x > 0 && distanceFromEdge <= EGDE_DETECTION_MARGIN);
	}

	private boolean onRightEdgeOfScreen(int x) {
		int currentPage = container.currentPage();
		
		int rightEdgeXCoor = (currentPage*gridPageWidth) + gridPageWidth;
		int distanceFromEdge = rightEdgeXCoor - x;
		return (x > (rightEdgeXCoor - EGDE_DETECTION_MARGIN) && distanceFromEdge < EGDE_DETECTION_MARGIN);
	}

	private void animateOnTheEdge() {
		View v = getChildAt(dragged);

		AnimationSet animSet = new AnimationSet(true);
		ScaleAnimation scale = new ScaleAnimation(.667f, 1.5f, .667f, 1.5f, v.getMeasuredWidth() * 3 / 4, v.getMeasuredHeight() * 3 / 4);
		scale.setDuration(200);
		scale.setRepeatMode(Animation.REVERSE);
		scale.setRepeatCount(Integer.MAX_VALUE);
		
		animSet.addAnimation(scale);

		v.clearAnimation();
		v.startAnimation(scale);
	}

	private void animateGap(int targetLocationInGrid) {
		int viewAtPosition = currentViewAtPosition(targetLocationInGrid);
		
		View targetView = getChildAt(viewAtPosition);

		Point oldXY = getCoorFromIndex(viewAtPosition);
		Point newXY = getCoorFromIndex(newPositions.get(dragged, dragged));

		Point oldOffset = computeTranslationStartDeltaRelativeToRealViewPosition(targetLocationInGrid, viewAtPosition, oldXY);	
		Point newOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, newXY);

		animateMoveToNewPosition(targetView, oldOffset, newOffset);
		saveNewPositions(targetLocationInGrid, viewAtPosition);
	}

	private Point computeTranslationEndDeltaRelativeToRealViewPosition(Point oldXY, Point newXY) {
		return new Point(newXY.x - oldXY.x, newXY.y - oldXY.y);
	}

	private Point computeTranslationStartDeltaRelativeToRealViewPosition(int targetLocation, int viewAtPosition, Point oldXY) {
		Point oldOffset;
		if (viewWasAlreadyMoved(targetLocation, viewAtPosition)) {
			Point targetLocationPoint = getCoorFromIndex(targetLocation);			
			oldOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, targetLocationPoint);
		} else {
			oldOffset = new Point(0,0);
		}
		return oldOffset;
	}

	private void saveNewPositions(int targetLocation, int viewAtPosition) {
		newPositions.put(viewAtPosition, newPositions.get(dragged, dragged));
		newPositions.put(dragged, targetLocation);
		swapDraggedWith(newPositions.get(dragged), newPositions.get(viewAtPosition));
	}

	private boolean viewWasAlreadyMoved(int targetLocation, int viewAtPosition) {
		return viewAtPosition != targetLocation;
	}

	private void animateMoveToNewPosition(View targetView, Point oldOffset, Point newOffset) {
		AnimationSet set = new AnimationSet(true);	
		
		Animation rotate = createFastRotateAnimation();        
		Animation translate = createTranslateAnimation(oldOffset, newOffset);
		
		set.addAnimation(rotate);
		set.addAnimation(translate);
		targetView.clearAnimation();
		targetView.startAnimation(set);
	}

	private TranslateAnimation createTranslateAnimation(Point oldOffset, Point newOffset) {
		TranslateAnimation translate = new TranslateAnimation(Animation.ABSOLUTE, oldOffset.x, 
															  Animation.ABSOLUTE, newOffset.x, 
															  Animation.ABSOLUTE, oldOffset.y, 
															  Animation.ABSOLUTE, newOffset.y);
		translate.setDuration(ANIMATION_DURATION);
		translate.setFillEnabled(true);
		translate.setFillAfter(true);
		return translate;
	}

	private Animation createFastRotateAnimation() {
		Animation a = new RotateAnimation(-2.0f, 
										  2.0f,
										  Animation.RELATIVE_TO_SELF, 
										  0.5f, 
										  Animation.RELATIVE_TO_SELF,
										  0.5f);		
	 	a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(Integer.MAX_VALUE);
        a.setDuration(60);
        
		return a;
	}

	private int currentViewAtPosition(int targetLocation) {
		int viewAtPosition = targetLocation;
		for (int i = 0; i < newPositions.size(); i++) {
			int value = newPositions.valueAt(i);
			if (value == targetLocation) {
				viewAtPosition = newPositions.keyAt(i);
				break;
			}
		}
		return viewAtPosition;
	}

	private Point getCoorFromIndex(int index) {
		ItemPosition page = getPageForItemAtAbsolutePosition(index);

		int row = page.itemIndex / computedColumnCount;
		int col = page.itemIndex - (row * computedColumnCount);

		int x = (getCurrentPage() * gridPageWidth) + (columnWidthSize * col);
		int y = rowHeightSize * row;
		
	
		return new Point(x, y);
	}

	private int getTargetFromCoor(int x, int y) {
		int page = getCurrentPage();

		int col = getColumnFromCoordinate(x, page);
		int row = getRowFromCoordinate(y);
		int positionInPage = col + (row * computedColumnCount);	

		return absolutePositionOfItemLocatedInPage(page, positionInPage);
	}

	private int getColumnFromCoordinate(int x, int page) {
		int col = 0;
		int pageLeftBorder = (page) * gridPageWidth;
		for (int i = 1; i <= computedColumnCount; i++) {
			int colRightBorder = (i * columnWidthSize) + pageLeftBorder;
			if (x < colRightBorder) {
				break;
			}
			col++;
		}
		return col;
	}

	private int getRowFromCoordinate(int y) {
		int row = 0;
		for (int i = 1; i <= computedRowCount; i++) {
			if (y < i * rowHeightSize) {
				break;
			}
			row++;
		}
		return row;
	}

	private int getCurrentPage() {
		return container.currentPage();
	}

	private void reorderChildren() {
		List<View> children = saveChildren();
		removeAllViews();
		reAddChildrenInParent(children);
		onLayout(true, getLeft(), getTop(), getRight(), getBottom());
	}

	private void reAddChildrenInParent(List<View> children) {
		List<View> reorderedViews = reeorderView(children);
		newPositions.clear();

		for (View view : reorderedViews) {
			addView(view);
		}
	}

	private List<View> saveChildren() {
		List<View> children = new ArrayList<View>();
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).clearAnimation();
			children.add(getChildAt(i));
		}
		return children;
	}

	private List<View> reeorderView(List<View> children) {
		View[] views = new View[children.size()];

		for (int i = 0; i < children.size(); i++) {
			int position = newPositions.get(i, -1);
			if (childHasMoved(position)) {
				views[position] = children.get(i);
			} else {
				views[i] = children.get(i);
			}
		}
		return new ArrayList<View>(Arrays.asList(views));
	}

	private boolean childHasMoved(int position) {
		return position != -1;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();

		widthSize = acknowledgeWidthSize(widthMode, widthSize, display);
		heightSize = acknowledgeHeightSize(heightMode, heightSize, display);

		adaptChildrenMeasuresToViewSize(widthSize, heightSize);
		computeBiggestChildMeasures();
		computeGridMatrixSize(widthSize, heightSize);
		computeColumnsAndRowsSizes(widthSize, heightSize);

		setMeasuredDimension(widthSize * adapter.pageCount(), heightSize);
	}

	private void computeColumnsAndRowsSizes(int widthSize, int heightSize) {
		columnWidthSize = widthSize / computedColumnCount;
		rowHeightSize = heightSize / computedRowCount;
	}

	private void computeGridMatrixSize(int widthSize, int heightSize) {
		if (adapter.columnCount() != -1 && adapter.rowCount() != -1) {
			computedColumnCount = adapter.columnCount();
			computedRowCount = adapter.rowCount();
		} else {
			computedColumnCount = widthSize / biggestChildWidth;			
			computedRowCount = heightSize / biggestChildHeight;			
		}
		
		if (computedColumnCount == 0) {
			computedColumnCount = 1;
		}
		
		if (computedRowCount == 0) {
			computedRowCount = 1;
		}
	}

	private void computeBiggestChildMeasures() {
		biggestChildWidth = 0;
		biggestChildHeight = 0;
		for (int index = 0; index < getChildCount(); index++) {
			View child = getChildAt(index);

			if (biggestChildHeight < child.getMeasuredHeight()) {
				biggestChildHeight = child.getMeasuredHeight();
			}

			if (biggestChildWidth < child.getMeasuredWidth()) {
				biggestChildWidth = child.getMeasuredWidth();
			}
		}
	}

	private void adaptChildrenMeasuresToViewSize(int widthSize, int heightSize) {
		if (adapter.columnCount() != -1 && adapter.rowCount() != -1) {
			int desiredGridItemWidth = widthSize / adapter.columnCount();
			int desiredGridItemHeight = heightSize / adapter.rowCount();			
			measureChildren(MeasureSpec.makeMeasureSpec(desiredGridItemWidth, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(desiredGridItemHeight, MeasureSpec.AT_MOST));
		} else {
			measureChildren(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		}
	}

	private int acknowledgeHeightSize(int heightMode, int heightSize, Display display) {
		if (heightMode == MeasureSpec.UNSPECIFIED) {
			heightSize = display.getHeight();
		}
		return heightSize;
	}

	private int acknowledgeWidthSize(int widthMode, int widthSize, Display display) {
		if (widthMode == MeasureSpec.UNSPECIFIED) {
			widthSize = display.getWidth();
		}
		gridPageWidth = widthSize;
		return widthSize;
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int pageWidth  = (l + r) / adapter.pageCount();		

		for (int page = 0; page < adapter.pageCount(); page++) {
			layoutPage(pageWidth, page);
		}
	}

	private void layoutPage(int pageWidth, int page) {
		int col = 0;
		int row = 0;
		for (int childIndex = 0; childIndex < adapter.itemCountInPage(page); childIndex++) {
			layoutAChild(pageWidth, page, col, row, childIndex);
			col++;
			if (col == computedColumnCount) {
				col = 0;
				row++;
			}
		}
	}

	private void layoutAChild(int pageWidth, int page, int col, int row, int childIndex) {
		int absoluteIndex = getViewIndex(page, childIndex);

		View child = getChildAt(absoluteIndex);
		
		int left = 0;
		int top = 0;
		if (absoluteIndex == dragged && lastTouchOnEdge()) {
			left = computePageEdgeXCoor(child); 
			top = lastTouchY - (child.getMeasuredHeight() / 2);
		} else {
			left = (page * pageWidth) + (col * columnWidthSize) + ((columnWidthSize - child.getMeasuredWidth()) / 2);
			top = (row * rowHeightSize) + ((rowHeightSize - child.getMeasuredHeight()) / 2);
		}
		child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
	}

	private boolean lastTouchOnEdge() {
		return onRightEdgeOfScreen(lastTouchX) || onLeftEdgeOfScreen(lastTouchX);
	}

	private int computePageEdgeXCoor(View child) {
		int left;
		left = lastTouchX - (child.getMeasuredWidth() / 2);	
		if (onRightEdgeOfScreen(lastTouchX)) {
			left = left - gridPageWidth;
		} else if (onLeftEdgeOfScreen(lastTouchX)) {
			left = left + gridPageWidth;
		}
		return left;
	}

	@Override
	public boolean onLongClick(View v) {
		container.disableScroll();

		movingView = true;
		dragged = getPositionForView(v);
		
		animateMoveAllItems();

		return true;
	}

	private int getPositionForView(View v) {
		for (int index = 0; index < getChildCount(); index++) {
			View child = getChildAt(index);
			if (isPointInsideView(initialX, initialY, child)) {
				return index;
			}
		}
		return -1;
	}

	private boolean isPointInsideView(float x, float y, View view) {
		int location[] = new int[2];
		view.getLocationOnScreen(location);
		int viewX = location[0];
		int viewY = location[1];

		// point is inside view bounds
		if ((x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()))) {
			return true;
		} else {
			return false;
		}
	}

	public void setContainer(PagedDragDropGrid container) {
		this.container = container;
	}
	
	
	
	
	
	public int getViewIndex(int pageIndex, int childIndex) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (pageIndex == currentPageIndex && childIndex == currentItemIndex) {
					return currentGlobalIndex;
				}
				currentGlobalIndex++;				
			}
		}

		return -1;
	}
	
	public List<Integer> absolutePositionOfItemsInPageContainingAbsolutePositionItem(int position) {
		List<Integer> indexes = new ArrayList<Integer>();
		int pageIndex = getPageIndexForAbsoluteItemPosition(position);
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (pageIndex == currentPageIndex) {
					indexes.add(currentGlobalIndex);
				}
				currentGlobalIndex++;				
			}
		}

		return indexes;
	}
	
	public int absolutePositionOfItemLocatedInPage(int pageIndex, int itemIndex) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (currentPageIndex == pageIndex && currentItemIndex == itemIndex) {
					return currentGlobalIndex;
				}
				currentGlobalIndex++;				
			}
		}

		return -1;
	}
	
	private int getPageIndexForAbsoluteItemPosition(int position) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (currentGlobalIndex == position) {
					return currentPageIndex;
				}
				currentGlobalIndex++;				
			}
		}

		return -1;
	}
	
	public ItemPosition getPageForItemAtAbsolutePosition(int position) {
		int currentGlobalIndex = 0;
		for (int currentPageIndex = 0; currentPageIndex < adapter.pageCount(); currentPageIndex++) {
			int itemCount = adapter.itemCountInPage(currentPageIndex);
			for (int currentItemIndex = 0; currentItemIndex < itemCount; currentItemIndex++) {
				if (currentGlobalIndex == position) {
					return new ItemPosition(currentPageIndex, currentItemIndex);
				}
				currentGlobalIndex++;				
			}
		}
		return null;
	}
	
	public int itemIndexInPage(int absoluteIndex) {
		return getPageIndexForAbsoluteItemPosition(absoluteIndex);
	}
	
	public void swapDraggedWith(int dragged, int target) {
		
		ItemPosition draggedItemPositionInPage = getPageForItemAtAbsolutePosition(dragged);
		ItemPosition targetItemPositionInPage = getPageForItemAtAbsolutePosition(target);
		if (draggedItemPositionInPage != null && targetItemPositionInPage != null) {
			adapter.swapItems(draggedItemPositionInPage.pageIndex,draggedItemPositionInPage.itemIndex, targetItemPositionInPage.itemIndex);
		}
	}


	public void moveItemToPageOnLeft(int itemIndex) {
		ItemPosition itemPosition = getPageForItemAtAbsolutePosition(itemIndex);
		adapter.moveItemToPageOnLeft(itemPosition.pageIndex,itemPosition.itemIndex);
			
	}


	public void moveItemToPageOnRight(int itemIndex) {
		ItemPosition itemPosition = getPageForItemAtAbsolutePosition(itemIndex);
		adapter.moveItemToPageOnRight(itemPosition.pageIndex,itemPosition.itemIndex);
	}
	
	public class ItemPosition {
		public int pageIndex;
		public int itemIndex;
		
		public ItemPosition(int pageIndex, int itemIndex) {
			super();
			this.pageIndex = pageIndex;
			this.itemIndex = itemIndex;
		}
	}
	
}
