/**
 * Copyright 2012
 *
 * Nicolas Desjardins
 * https://github.com/mrKlar
 *
 * Facilite solutions
 * http://www.facilitesolutions.com/
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ca.laplanete.mobile.pageddragdropgrid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DragDropGrid extends ViewGroup implements OnTouchListener, OnLongClickListener {

	private static int ANIMATION_DURATION = 250;
	private static int EGDE_DETECTION_MARGIN = 35;

	private PagedDragDropGridAdapter adapter;
	private OnClickListener onClickListener = null;
	private PagedContainer container;

	private SparseArray<Integer> newPositions = new SparseArray<Integer>();

	private int gridPageWidth = 0;
	private int dragged = -1;
	private int columnWidthSize;
	private int rowHeightSize;
	private int biggestChildWidth;
	private int biggestChildHeight;
	private int computedColumnCount;
	private int computedRowCount;
	private int initialX;
	private int initialY;
	private boolean movingView;
	private int lastTarget = -1;
	private boolean wasOnEdgeJustNow = false;
	private Timer edgeScrollTimer;

	final private Handler edgeTimerHandler = new Handler();
	private int lastTouchX;
	private int lastTouchY;
	private int gridPageHeight;
	private DeleteDropZoneView deleteZone;

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
		createDeleteZone();
	}

	public void setAdapter(PagedDragDropGridAdapter adapter) {
		this.adapter = adapter;
		addChildViews();
	}

	public void setOnClickListener(OnClickListener l) {
	    onClickListener = l;
	}

	private void addChildViews() {
		for (int page = 0; page < adapter.pageCount(); page++) {
			for (int item = 0; item < adapter.itemCountInPage(page); item++) {
				addView(adapter.view(page, item));
			}
		}
		deleteZone.bringToFront();
	}

	private void animateMoveAllItems() {
		Animation rotateAnimation = createFastRotateAnimation();

		for (int i=0; i < getItemViewCount(); i++) {
			View child = getChildAt(i);
			child.startAnimation(rotateAnimation);
		 }
	}

	private void cancelAnimations() {
		 for (int i=0; i < getItemViewCount(); i++) {
			 if (i != dragged) {
				 View child = getChildAt(i);
				 child.clearAnimation();
			 }
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
			touchUp(event);
			break;
		}
		if (aViewIsDragged())
			return true;
		return false;
	}

	private void touchUp(MotionEvent event) {
	    if(dragged == -1) {
	        if(onClickListener != null) {
                View clickedView = getChildAt(getTargetAtCoor((int) event.getX(), (int) event.getY()));
                if(clickedView != null)
                    onClickListener.onClick(clickedView);
            }
	    } else {
    		manageChildrenReordering();
    		hideDeleteView();
    		cancelEdgeTimer();

    		movingView = false;
    		dragged = -1;
    		lastTarget = -1;
    		container.enableScroll();
    		cancelAnimations();
	    }
	}

	private void manageChildrenReordering() {
		boolean draggedDeleted = touchUpInDeleteZoneDrop(lastTouchX, lastTouchY);

		if (draggedDeleted) {
			animateDeleteDragged();
			reorderChildrenWhenDraggedIsDeleted();
		} else {
			reorderChildren();
		}
	}

	private void animateDeleteDragged() {
		ScaleAnimation scale = new ScaleAnimation(1.4f, 0f, 1.4f, 0f, biggestChildWidth / 2 , biggestChildHeight / 2);
		scale.setDuration(200);
		scale.setFillAfter(true);
		scale.setFillEnabled(true);

		getChildAt(dragged).clearAnimation();
		getChildAt(dragged).startAnimation(scale);
	}

	private void reorderChildrenWhenDraggedIsDeleted() {
		Integer newDraggedPosition = newPositions.get(dragged,dragged);

		List<View> children = cleanUnorderedChildren();
		addReorderedChildrenToParent(children);

		tellAdapterDraggedIsDeleted(newDraggedPosition);
		removeViewAt(newDraggedPosition);

		requestLayout();
	}

	private void tellAdapterDraggedIsDeleted(Integer newDraggedPosition) {
		ItemPosition position = itemInformationAtPosition(newDraggedPosition);
		adapter.deleteItem(position.pageIndex,position.itemIndex);
	}

	private void touchDown(MotionEvent event) {
		initialX = (int)event.getRawX();
		initialY = (int)event.getRawY();

		lastTouchX = (int)event.getRawX() + (currentPage() * gridPageWidth);
		lastTouchY = (int)event.getRawY();
	}

	private void touchMove(MotionEvent event) {
		if (movingView && aViewIsDragged()) {
			lastTouchX = (int) event.getX();
			lastTouchY = (int) event.getY();

			moveDraggedView(lastTouchX, lastTouchY);
			manageSwapPosition(lastTouchX, lastTouchY);
			manageEdgeCoordinates(lastTouchX);
			manageDeleteZoneHover(lastTouchX, lastTouchY);
		}
	}

	private void manageDeleteZoneHover(int x, int y) {
		Rect zone = new Rect();
		deleteZone.getHitRect(zone);

		if (zone.intersect(x, y, x+1, y+1)) {
			deleteZone.highlight();
		} else {
			deleteZone.smother();
		}
	}

	private boolean touchUpInDeleteZoneDrop(int x, int y) {
		Rect zone = new Rect();
		deleteZone.getHitRect(zone);

		if (zone.intersect(x, y, x+1, y+1)) {
			deleteZone.smother();
			return true;
		}
		return false;
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
		int target = getTargetAtCoor(x, y);
		if (childHasMoved(target) && target != lastTarget) {
			animateGap(target);
			lastTarget = target;
		}
	}

	private void manageEdgeCoordinates(int x) {
		final boolean onRightEdge = onRightEdgeOfScreen(x);
		final boolean onLeftEdge = onLeftEdgeOfScreen(x);

		if (canScrollToEitherSide(onRightEdge,onLeftEdge)) {
			if (!wasOnEdgeJustNow) {
				startEdgeDelayTimer(onRightEdge, onLeftEdge);
				wasOnEdgeJustNow = true;
			}
		} else {
			if (wasOnEdgeJustNow) {
				stopAnimateOnTheEdge();
			}
			wasOnEdgeJustNow = false;
			cancelEdgeTimer();
		}
	}

	private void stopAnimateOnTheEdge() {
			View draggedView = getChildAt(dragged);
			draggedView.clearAnimation();
			animateDragged();
	}

	private void cancelEdgeTimer() {

		if (edgeScrollTimer != null) {
			edgeScrollTimer.cancel();
			edgeScrollTimer = null;
		}
	}

	private void startEdgeDelayTimer(final boolean onRightEdge, final boolean onLeftEdge) {
		if (canScrollToEitherSide(onRightEdge, onLeftEdge)) {
			animateOnTheEdge();
			if (edgeScrollTimer == null) {
				edgeScrollTimer = new Timer();
				scheduleScroll(onRightEdge, onLeftEdge);
			}
		}
	}

	private void scheduleScroll(final boolean onRightEdge, final boolean onLeftEdge) {
		edgeScrollTimer.schedule(new TimerTask() {
		    @Override
		    public void run() {
		    	if (wasOnEdgeJustNow) {
		    		wasOnEdgeJustNow = false;
		    		edgeTimerHandler.post(new Runnable() {
						@Override
						public void run() {
							hideDeleteView();
							scroll(onRightEdge, onLeftEdge);
							cancelAnimations();
							animateMoveAllItems();
							animateDragged();
							popDeleteView();
						}
					});
		    	}
		    }
		}, 1000);
	}

	private boolean canScrollToEitherSide(final boolean onRightEdge, final boolean onLeftEdge) {
		return (onLeftEdge && container.canScrollToPreviousPage()) || (onRightEdge && container.canScrollToNextPage());
	}

	private void scroll(boolean onRightEdge, boolean onLeftEdge) {
		cancelEdgeTimer();

		if (onLeftEdge && container.canScrollToPreviousPage()) {
			scrollToPreviousPage();
		} else if (onRightEdge && container.canScrollToNextPage()) {
			scrollToNextPage();
		}
		wasOnEdgeJustNow = false;
	}

	private void scrollToNextPage() {
		tellAdapterToMoveItemToNextPage(dragged);
		moveDraggedToNextPage();

		container.scrollRight();
		int currentPage = currentPage();
		int lastItem = adapter.itemCountInPage(currentPage)-1;
		dragged = positionOfItem(currentPage, lastItem);

		requestLayout();
		stopAnimateOnTheEdge();
	}

	private void scrollToPreviousPage() {
		tellAdapterToMoveItemToPreviousPage(dragged);
		moveDraggedToPreviousPage();

		container.scrollLeft();
		int currentPage = currentPage();
		int lastItem = adapter.itemCountInPage(currentPage)-1;
		dragged = positionOfItem(currentPage, lastItem);

		requestLayout();
		stopAnimateOnTheEdge();
	}

	private void moveDraggedToPreviousPage() {
		List<View> children = cleanUnorderedChildren();

		List<View> reorderedViews = reeorderView(children);
		int draggedEndPosition = newPositions.get(dragged, dragged);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);

		int currentPage = currentPage();
		int indexFirstElementInCurrentPage = 0;
		for (int i=0;i<currentPage;i++) {
			indexFirstElementInCurrentPage += adapter.itemCountInPage(i);
		}

		int indexOfDraggedOnNewPage = indexFirstElementInCurrentPage-1;
		reorderAndAddViews(reorderedViews, draggedView, indexOfDraggedOnNewPage);
	}

	private void removeItemChildren(List<View> children) {
		for (View child : children) {
			removeView(child);
		}
	}

	private void moveDraggedToNextPage() {
		List<View> children = cleanUnorderedChildren();

		List<View> reorderedViews = reeorderView(children);
		int draggedEndPosition = newPositions.get(dragged, dragged);

		View draggedView = reorderedViews.get(draggedEndPosition);
		reorderedViews.remove(draggedEndPosition);

		int currentPage = currentPage();
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
			if (view != null) {
				addView(view);
			}
		}

		deleteZone.bringToFront();
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
		return (x > (rightEdgeXCoor - EGDE_DETECTION_MARGIN)) && (distanceFromEdge < EGDE_DETECTION_MARGIN);
	}

	private void animateOnTheEdge() {
		View v = getChildAt(dragged);

		ScaleAnimation scale = new ScaleAnimation(.667f, 1.5f, .667f, 1.5f, v.getMeasuredWidth() * 3 / 4, v.getMeasuredHeight() * 3 / 4);
		scale.setDuration(200);
		scale.setRepeatMode(Animation.REVERSE);
		scale.setRepeatCount(Animation.INFINITE);

		v.clearAnimation();
		v.startAnimation(scale);
	}

	private void animateGap(int targetLocationInGrid) {
		int viewAtPosition = currentViewAtPosition(targetLocationInGrid);

		if (viewAtPosition == dragged) {
			return;
		}

		View targetView = getChildAt(viewAtPosition);

		Point oldXY = getCoorForIndex(viewAtPosition);
		Point newXY = getCoorForIndex(newPositions.get(dragged, dragged));

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
			Point targetLocationPoint = getCoorForIndex(targetLocation);
			oldOffset = computeTranslationEndDeltaRelativeToRealViewPosition(oldXY, targetLocationPoint);
		} else {
			oldOffset = new Point(0,0);
		}
		return oldOffset;
	}

	private void saveNewPositions(int targetLocation, int viewAtPosition) {
		newPositions.put(viewAtPosition, newPositions.get(dragged, dragged));
		newPositions.put(dragged, targetLocation);
		tellAdapterToSwapDraggedWithTarget(newPositions.get(dragged, dragged), newPositions.get(viewAtPosition, viewAtPosition));
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
		translate.setInterpolator(new AccelerateDecelerateInterpolator());
		return translate;
	}

	private Animation createFastRotateAnimation() {
		Animation rotate = new RotateAnimation(-2.0f,
										  2.0f,
										  Animation.RELATIVE_TO_SELF,
										  0.5f,
										  Animation.RELATIVE_TO_SELF,
										  0.5f);

	 	rotate.setRepeatMode(Animation.REVERSE);
        rotate.setRepeatCount(Animation.INFINITE);
        rotate.setDuration(60);
        rotate.setInterpolator(new AccelerateDecelerateInterpolator());

		return rotate;
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

	private Point getCoorForIndex(int index) {
		ItemPosition page = itemInformationAtPosition(index);

		int row = page.itemIndex / computedColumnCount;
		int col = page.itemIndex - (row * computedColumnCount);

		int x = (currentPage() * gridPageWidth) + (columnWidthSize * col);
		int y = rowHeightSize * row;

		return new Point(x, y);
	}

	private int getTargetAtCoor(int x, int y) {
		int page = currentPage();

		int col = getColumnOfCoordinate(x, page);
		int row = getRowOfCoordinate(y);
		int positionInPage = col + (row * computedColumnCount);

		return positionOfItem(page, positionInPage);
	}

	private int getColumnOfCoordinate(int x, int page) {
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

	private int getRowOfCoordinate(int y) {
		int row = 0;
		for (int i = 1; i <= computedRowCount; i++) {
			if (y < i * rowHeightSize) {
				break;
			}
			row++;
		}
		return row;
	}

	private int currentPage() {
		return container.currentPage();
	}

	private void reorderChildren() {
		List<View> children = cleanUnorderedChildren();
		addReorderedChildrenToParent(children);
		requestLayout();
	}

	private List<View> cleanUnorderedChildren() {
		List<View> children = saveChildren();
		removeItemChildren(children);
		return children;
	}

	private void addReorderedChildrenToParent(List<View> children) {
		List<View> reorderedViews = reeorderView(children);
		newPositions.clear();

		for (View view : reorderedViews) {
			if (view != null)
				addView(view);
		}

		deleteZone.bringToFront();
	}

	private List<View> saveChildren() {
		List<View> children = new ArrayList<View>();
		for (int i = 0; i < getItemViewCount(); i++) {
			View child = getChildAt(i);
			child.clearAnimation();
			children.add(child);
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
		searchBiggestChildMeasures();
		computeGridMatrixSize(widthSize, heightSize);
		computeColumnsAndRowsSizes(widthSize, heightSize);

		measureChild(deleteZone, MeasureSpec.makeMeasureSpec(gridPageWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)getPixelFromDip(40), MeasureSpec.EXACTLY));

		setMeasuredDimension(widthSize * adapter.pageCount(), heightSize);
	}

	private float getPixelFromDip(int size) {
		Resources r = getResources();
		float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, r.getDisplayMetrics());
		return px;
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
			if (biggestChildWidth > 0 && biggestChildHeight > 0) {
				computedColumnCount = widthSize / biggestChildWidth;
				computedRowCount = heightSize / biggestChildHeight;
			}
		}

		if (computedColumnCount == 0) {
			computedColumnCount = 1;
		}

		if (computedRowCount == 0) {
			computedRowCount = 1;
		}
	}

	private void searchBiggestChildMeasures() {
		biggestChildWidth = 0;
		biggestChildHeight = 0;
		for (int index = 0; index < getItemViewCount(); index++) {
			View child = getChildAt(index);

			if (biggestChildHeight < child.getMeasuredHeight()) {
				biggestChildHeight = child.getMeasuredHeight();
			}

			if (biggestChildWidth < child.getMeasuredWidth()) {
				biggestChildWidth = child.getMeasuredWidth();
			}
		}
	}

	private int getItemViewCount() {
		// -1 to remove the DeleteZone from the loop
		return getChildCount()-1;
	}

	private void adaptChildrenMeasuresToViewSize(int widthSize, int heightSize) {
		if (adapter.columnCount() != PagedDragDropGridAdapter.AUTOMATIC && adapter.rowCount() != PagedDragDropGridAdapter.AUTOMATIC) {
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
		gridPageHeight = heightSize;
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
		int position = positionOfItem(page, childIndex);

		View child = getChildAt(position);

		int left = 0;
		int top = 0;
		if (position == dragged && lastTouchOnEdge()) {
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
		dragged = positionForView(v);

		animateMoveAllItems();

		animateDragged();
		popDeleteView();

		return true;
	}

	private void animateDragged() {

		ScaleAnimation scale = new ScaleAnimation(1f, 1.4f, 1f, 1.4f, biggestChildWidth / 2 , biggestChildHeight / 2);
		scale.setDuration(200);
		scale.setFillAfter(true);
		scale.setFillEnabled(true);

		if (aViewIsDragged()) {
			getChildAt(dragged).clearAnimation();
			getChildAt(dragged).startAnimation(scale);
		}
	}

	private boolean aViewIsDragged() {
		return dragged != -1;
	}

	private void popDeleteView() {

		deleteZone.setVisibility(View.VISIBLE);

		int l = currentPage() * deleteZone.getMeasuredWidth();
		int t = gridPageHeight - deleteZone.getMeasuredHeight();
		deleteZone.layout(l,  t, l + gridPageWidth, t + gridPageHeight);
	}

	private void createDeleteZone() {
		deleteZone = new DeleteDropZoneView(getContext());
		addView(deleteZone);
	}

	private void hideDeleteView() {
		deleteZone.setVisibility(View.GONE);
	}

	private int positionForView(View v) {
		for (int index = 0; index < getItemViewCount(); index++) {
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

		if (pointIsInsideViewBounds(x, y, view, viewX, viewY)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean pointIsInsideViewBounds(float x, float y, View view, int viewX, int viewY) {
		return (x > viewX && x < (viewX + view.getWidth())) && (y > viewY && y < (viewY + view.getHeight()));
	}

	public void setContainer(PagedDragDropGrid container) {
		this.container = container;
	}

	private int positionOfItem(int pageIndex, int childIndex) {
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

	private ItemPosition itemInformationAtPosition(int position) {
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

	private void tellAdapterToSwapDraggedWithTarget(int dragged, int target) {
		ItemPosition draggedItemPositionInPage = itemInformationAtPosition(dragged);
		ItemPosition targetItemPositionInPage = itemInformationAtPosition(target);
		if (draggedItemPositionInPage != null && targetItemPositionInPage != null) {
			adapter.swapItems(draggedItemPositionInPage.pageIndex,draggedItemPositionInPage.itemIndex, targetItemPositionInPage.itemIndex);
		}
	}

	private void tellAdapterToMoveItemToPreviousPage(int itemIndex) {
		ItemPosition itemPosition = itemInformationAtPosition(itemIndex);
		adapter.moveItemToPreviousPage(itemPosition.pageIndex,itemPosition.itemIndex);
	}

	private void tellAdapterToMoveItemToNextPage(int itemIndex) {
		ItemPosition itemPosition = itemInformationAtPosition(itemIndex);
		adapter.moveItemToNextPage(itemPosition.pageIndex,itemPosition.itemIndex);
	}

	private class ItemPosition {
		public int pageIndex;
		public int itemIndex;

		public ItemPosition(int pageIndex, int itemIndex) {
			super();
			this.pageIndex = pageIndex;
			this.itemIndex = itemIndex;
		}
	}
}
