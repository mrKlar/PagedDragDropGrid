Note: This component is not in development anymore.  I will merge pull requests, but that’s about it.  

======================
PagedDragDropGrid v0.2 (deprecated)
======================

An Android ViewGroup that implements a paged grid with drag'n'drop movable items

Supports Android 2.2 (API 8) and up

Tested on a Nexus One, Galaxy Nexus and a Nexus 7

Example video: 

v0.1 : http://youtu.be/FYTSRfthSuQ

v0.2 : http://youtu.be/0HI2meKKLYk


Usage
-----

Define an adapter conforming to interface PagedDragDropGridAdapter.java

```java
public interface PagedDragDropGridAdapter {

    // Automatic child distribution
	public final static int AUTOMATIC = -1; 
	
	// Delete drop zone location TOP
	public final static int TOP = 1;
	
	// Delete drop zone location BOTTOM
	public final static int BOTTOM = 2;
	
	/**
	 * Used to create the paging
	 * 
	 * @return the page count
	 */
	public int pageCount();

	/**
	 * Returns the count of item in a page
	 * 
	 * @param page index
	 * @return item count for page
	 */
	public int itemCountInPage(int page);
	
	/**
	 * Returns the view for the item in the page
	 * 
	 * @param page index
	 * @param item index
	 * @return the view 
	 */
	public View view(int page, int index);
	
	/**
	 * The fixed row count (AUTOMATIC for automatic computing)
	 * 
	 * @return row count or AUTOMATIC
	 */
	public int rowCount();
	
	/**
	 * The fixed column count (AUTOMATIC for automatic computing)
	 * 
	 * @return column count or AUTOMATIC
	 */
	public int columnCount();

	/**
	 * Prints the layout in Log.d();
	 */
	public void printLayout();

	/**
	 * Swaps two items in the item list in a page
	 * 
	 * @param pageIndex
	 * @param itemIndexA
	 * @param itemIndexB
	 */
	public void swapItems(int pageIndex, int itemIndexA, int itemIndexB);

	/**
	 * Moves an item in the page on the left of provided the page
	 * 
	 * @param pageIndex
	 * @param itemIndex
	 */
	public void moveItemToPreviousPage(int pageIndex, int itemIndex);

	/**
	 * Moves an item in the page on the right of provided the page
	 * 
	 * @param pageIndex
	 * @param itemIndex
	 */
	public void moveItemToNextPage(int pageIndex, int itemIndex);

	
	/**
	 * deletes the item in page and at position
	 * 
	 * @param pageIndex
	 * @param itemIndex
	 */
	public void deleteItem(int pageIndex, int itemIndex);

	/** 
	 * Returns the delete drop zone location.  
	 * 
	 * @return TOP or BOTTOM. 
	 */
	public int deleteDropZoneLocation();

	/**
	 * Tells the grid to show or not the remove drop zone when moving an item
 	 */
	public boolean showRemoveDropZone();
}
```
	

layout example.xml
----------
```xml
<ca.laplanete.mobile.pageddragdropgrid.PagedDragDropGrid xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/gridview"
    android:layout_width="fill_parent"
    android:layout_height="fill_parent"/>
```
    
    
ExampleActivity.java
-------------
```java
setContentView(R.layout.example);
PagedDragDropGrid gridview = (PagedDragDropGrid) findViewById(R.id.gridview);		
gridview.setAdapter(new ExamplePagedDragDropGridAdapter(this));

/* Optionally set an onClickListener */
gridview.setClickListener(this);

/* Optionally set an setOnPageChangedListener */
gridview.setOnPageChangedListener(new OnPageChangedListener() {            
	@Override
	public void onPageChanged(PagedDragDropGrid sender, int newPageNumber) {
	        Toast.makeText(ExampleActivity.this, "Page changed to page " + newPageNumber, Toast.LENGTH_SHORT).show();                
	    }
	});

```

Inspired by
-----------

https://github.com/thquinn/DraggableGridView
and
http://blahti.wordpress.com/2011/10/03/drag-drop-for-android-gridview/

Changelog
---------

0.2
---

- The dragged view is now in front of the others

0.1
---

- Initial version
- Bug fixing

License
-------
```java
/**
   * Copyright 2012 
   * 
   * Nicolas Desjardins  
   * https://github.com/mrKlar
   * 
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
```
