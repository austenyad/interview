ActivityThread.handleResumeActivity

WindowManagerImpl.addView(view，params)

WindowManagerGlobal.addView 单例类

ViewRootImpl.setView

requestLayout() ，从上往下触发第一次 View 的绘制，在 requestLayout() 方法中。

view.assignParent(ViewRootImpl) ：设置 DecorView 的父 View 为 ViewRootImpl，ViewRootImpl 它其实不是一个 View。这里暂时理解 ViewRootImpl 为 DecorView 的 父亲，在 DecorView 的顶层来处理 DecorView。

因为 DecorView 是我们的布局顶层，现在我们知道层层调用 requestLayout 等方法是怎么调到 ViewRootImpl 里的。

#### 4. DecorView 的布局是什么样的？

对于 Activity 的层级，大家应该都看过一种描述，Activity -> PhoneWindow -> DecorView -> [title_bar,content]，其中 DecorView 里面包括 title_bar 和 content 两个 View ，不过这个是默认布局，实际上根据不同的主题样式，DecorView 对应有不同的布局。

图中所包含的 title_bar 和content 对应的是 R.layout.screen_simple 布局。

那么多布局是什么时候设置的呢？

是在 PhoneWindow.installDecor -> generateLayout 中设置的。

```java
    private void installDecor() {
        mForceDecorInstall = false;
        if (mDecor == null) {
          	// 生成 DecorView
            mDecor = generateDecor(-1);
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            if (!mInvalidatePanelMenuPosted && mInvalidatePanelMenuFeatures != 0) {
                mDecor.postOnAnimation(mInvalidatePanelMenuRunnable);
            }
        } else {
            mDecor.setWindow(this);
        }
        if (mContentParent == null) {
           // 生成 DecorView 的子 View: mContentParent(content)
            mContentParent = generateLayout(mDecor);
        }
    }


 protected ViewGroup generateLayout(DecorView decor) {
  	/..../
      // 根据不同的 window feature 给 DecorView 设置不同的布局
    int layoutResource;
        int features = getLocalFeatures();
        // System.out.println("Features: 0x" + Integer.toHexString(features));
        if ((features & (1 << FEATURE_SWIPE_TO_DISMISS)) != 0) {
            layoutResource = R.layout.screen_swipe_dismiss;
            setCloseOnSwipeEnabled(true);
        } else if ((features & ((1 << FEATURE_LEFT_ICON) | (1 << FEATURE_RIGHT_ICON))) != 0) {
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogTitleIconsDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_title_icons;
            }
            // XXX Remove this once action bar supports these features.
            removeFeature(FEATURE_ACTION_BAR);
            // System.out.println("Title Icons!");
        } else if ((features & ((1 << FEATURE_PROGRESS) | (1 << FEATURE_INDETERMINATE_PROGRESS))) != 0
                && (features & (1 << FEATURE_ACTION_BAR)) == 0) {
            // Special case for a window with only a progress bar (and title).
            // XXX Need to have a no-title version of embedded windows.
            layoutResource = R.layout.screen_progress;
            // System.out.println("Progress!");
        } else if ((features & (1 << FEATURE_CUSTOM_TITLE)) != 0) {
            // Special case for a window with a custom title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogCustomTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else {
                layoutResource = R.layout.screen_custom_title;
            }
            // XXX Remove this once action bar supports these features.
            removeFeature(FEATURE_ACTION_BAR);
        } else if ((features & (1 << FEATURE_NO_TITLE)) == 0) {
            // If no other features and not embedded, only need a title.
            // If the window is floating, we need a dialog layout
            if (mIsFloating) {
                TypedValue res = new TypedValue();
                getContext().getTheme().resolveAttribute(
                        R.attr.dialogTitleDecorLayout, res, true);
                layoutResource = res.resourceId;
            } else if ((features & (1 << FEATURE_ACTION_BAR)) != 0) {
                layoutResource = a.getResourceId(
                        R.styleable.Window_windowActionBarFullscreenDecorLayout,
                        R.layout.screen_action_bar);
            } else {
                layoutResource = R.layout.screen_title;
            }
            // System.out.println("Title!");
        } else if ((features & (1 << FEATURE_ACTION_MODE_OVERLAY)) != 0) {
            layoutResource = R.layout.screen_simple_overlay_action_mode;
        } else {
            // Embedded, so no decoration is needed.
         		// 默认布局
            layoutResource = R.layout.screen_simple;
            // System.out.println("Simple!");
        }

        mDecor.startChanging();
        mDecor.onResourcesLoaded(mLayoutInflater, layoutResource);

   // ID_ANDROID_CONTENT = com.android.internal.R.id.content; 是 R.layout.screen_simple; 里面
   // 的 View 的 id,这是一个 FrameLayout
    ViewGroup contentParent = (ViewGroup)findViewById(ID_ANDROID_CONTENT);
   
   return contentParent;
 }


 void onResourcesLoaded(LayoutInflater inflater, int layoutResource) {
       // 根据 上一步选择的 layoutResource 生成 View
        mDecorCaptionView = createDecorCaptionView(inflater);
        final View root = inflater.inflate(layoutResource, null);
        if (mDecorCaptionView != null) {
            if (mDecorCaptionView.getParent() == null) {
                addView(mDecorCaptionView,
                        new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            }
            mDecorCaptionView.addView(root,
                    new ViewGroup.MarginLayoutParams(MATCH_PARENT, MATCH_PARENT));
        } else {

            // Put it below the color views.
            // 添加到 DecorView 里
            addView(root, 0, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        }
        mContentRoot = (ViewGroup) root;
        initializeElevation();
    }
```



#### 5. DecorView 的创建时机

上面说 DecorView 布局的时候，其实我们也看到了，在 PhoneWindow.installDecor -> generateDecor 其实就是创建 DecorView。

那么 installDecor 是什么时候调用的呢？

调用链是 Activity.setContentView -> PhoneWinow.setContentView -> installDecor

说到这里那就继续会想到，Activity.setContentView 的流程是什么？

#### 6. Activity.setContentView 的流程

setContentView 的流程比较简单，会调用 PhoneWindow.setContentView。

在其中做的事是两个：

1. 创建 DecorView
2. 根据 layoutResId 创建 View 并添加到 DecorView 中。

```java
//PhoneWindow.java 
@Override
    public void setContentView(int layoutResID) {
        // Note: FEATURE_CONTENT_TRANSITIONS may be set in the process of installing the window
        // decor, when theme attributes and the like are crystalized. Do not check the feature
        // before this happens.
        if (mContentParent == null) {
          	// 创建 DecorView，并将 
            installDecor();
        } else if (!hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            mContentParent.removeAllViews();
        }

        if (hasFeature(FEATURE_CONTENT_TRANSITIONS)) {
            final Scene newScene = Scene.getSceneForLayout(mContentParent, layoutResID,
                    getContext());
            transitionTo(newScene);
        } else {
           // 将 我们的布局添加到 content 中，完成布局加载
            mLayoutInflater.inflate(layoutResID, mContentParent);
        }
        mContentParent.requestApplyInsets();
        final Callback cb = getCallback();
        if (cb != null && !isDestroyed()) {
            cb.onContentChanged();
        }
        mContentParentExplicitlySet = true;
    }

```

#### 7. LayoutInflate 的流程

既然上一步用到 LayoutInflate.inflate，那么 LayoutInflate.inflate 加载布局的流程是什么样的呢？

```java
  public View inflate(@LayoutRes int resource, @Nullable ViewGroup root, boolean attachToRoot) {
        final Resources res = getContext().getResources();
        if (DEBUG) {
            Log.d(TAG, "INFLATING from resource: \"" + res.getResourceName(resource) + "\" ("
                  + Integer.toHexString(resource) + ")");
        }

        View view = tryInflatePrecompiled(resource, res, root, attachToRoot);
        if (view != null) {
            return view;
        }
    	 // 更加 xml resourceId 获取对于的 xml 资源解析器
        XmlResourceParser parser = res.getLayout(resource);
        try {
            return inflate(parser, root, attachToRoot);
        } finally {
            parser.close();
        }
    }


    private void advanceToRootNode(XmlPullParser parser)
        throws InflateException, IOException, XmlPullParserException {
        // Look for the root node.
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG &&
            type != XmlPullParser.END_DOCUMENT) {
            // Empty
        }

        if (type != XmlPullParser.START_TAG) {
            throw new InflateException(parser.getPositionDescription()
                + ": No start tag found!");
        }
    }


  public View inflate(XmlPullParser parser, @Nullable ViewGroup root, boolean attachToRoot) {
        synchronized (mConstructorArgs) {
            

            //..
            View result = root;

            try {
              	// 解析器预先在 xml 文件里面寻找 根节点
                advanceToRootNode(parser);
                final String name = parser.getName();
								// 处理 merge 标签
                if (TAG_MERGE.equals(name)) {
                    if (root == null || !attachToRoot) {
                        throw new InflateException("<merge /> can be used only with a valid "
                                + "ViewGroup root and attachToRoot=true");
                    }

                    rInflate(parser, root, inflaterContext, attrs, false);
                } else {
                    // Temp is the root view that was found in the xml
                  	// 通过 tag 创建 View
                    final View temp = createViewFromTag(root, name, inflaterContext, attrs);

                    ViewGroup.LayoutParams params = null;

                    if (root != null) {
                        // 获取 root 的 LayoutParams
                        params = root.generateLayoutParams(attrs);
                        if (!attachToRoot) {
                            // Set the layout params for temp if we are not
                            // attaching. (If we are, we use addView, below)
                            temp.setLayoutParams(params);
                        }
                    }

                    if (DEBUG) {
                        System.out.println("-----> start inflating children");
                    }

                    // Inflate all children under temp against its context.
                    rInflateChildren(parser, temp, attrs, true);

                    if (DEBUG) {
                        System.out.println("-----> done inflating children");
                    }

                    // We are supposed to attach all the views we found (int temp)
                    // to root. Do that now.
                    if (root != null && attachToRoot) {
                        root.addView(temp, params);
                    }

                    // Decide whether to return the root that was passed in or the
                    // top view found in xml.
                    if (root == null || !attachToRoot) {
                        result = temp;
                    }
                }

            } catch (XmlPullParserException e) {
                final InflateException ie = new InflateException(e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } catch (Exception e) {
                final InflateException ie = new InflateException(
                        getParserStateDescription(inflaterContext, attrs)
                        + ": " + e.getMessage(), e);
                ie.setStackTrace(EMPTY_STACK_TRACE);
                throw ie;
            } finally {
                // Don't retain static reference on context.
                mConstructorArgs[0] = lastContext;
                mConstructorArgs[1] = null;

                Trace.traceEnd(Trace.TRACE_TAG_VIEW);
            }

            return result;
        }
    }


 final void rInflateChildren(XmlPullParser parser, View parent, AttributeSet attrs,
            boolean finishInflate) throws XmlPullParserException, IOException {
        rInflate(parser, parent, parent.getContext(), attrs, finishInflate);
    }


```



```java

//file: LayoutInflate.inflate
View result = root;

final View temp = createViewFromTag(root,name,inflaterContext,attrs);
if(root != null){
	params = root.generateLayoutParams(attrs);
	if(!attachToRoot){
		temp.setLayoutParams(params);
	}
}
 rInflateChildren(parser, temp, attrs, true);
 
 if(root != null && attachToRoot){
 	root.addView(temp,params);
 }
 
 if(root == null || !attachToRoot){
 	 result = temp;
 }
 
 return result;
```



1. 
2. root !=  null / attachToRoot = true

表示布局 resourceId 生成的 View 添加到 root 中，添加过程中 resource 所指定的布局的根节点的各个属性都是有效的。

2. root != null / attachToRoot = false 

表示 布局 resource 生成的 View 不添加到 root 中，这里有个疑问：既然不添加到 root 中，为什么还要写这么多条件。这里涉及另外一个问题？

我们在开发过程中指定的 layout_width 和 layout_height 到底是什么意思？

该属性表示一个控件在容器中的大小，就是说这个控件必须在容器中，这个属性才有意义，否则无意义。这就意味这我们直接将 resource 加载进来而不给它指定父布局，则 infalte 布局的根节点的 layout_width 和 layout_height 属性就会失效（因为要加载的布局不会在任何容器中，那么它的根节点的宽高自然会失效）如果我想让 根节点有效，又不想其处于某一个容器中，那我就可以设置 root 不为 null，而 attatchToRoot 为 false。这样 ，指定 root 的目的就是协助 要加载布局的根节点生成布局参数，只有这一个作用。

3. root == null

当 root 为 null 时，不论 attachToRoot 为 true 还是为 false，显示效果都是一样的。当 root 为 null 表示我不需要将第一个参数所指定的布局添加到任何容器当中，同时也表示没有任何容器来协助第一个参数所指定的布局的根节点生成布局参数。