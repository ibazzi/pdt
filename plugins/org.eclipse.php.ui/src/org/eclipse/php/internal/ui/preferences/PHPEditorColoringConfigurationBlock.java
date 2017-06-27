package org.eclipse.php.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.*;
import org.eclipse.php.internal.core.documentModel.provisional.contenttype.ContentTypeIdForPHP;
import org.eclipse.php.internal.ui.ColorManager;
import org.eclipse.php.internal.ui.PHPUIMessages;
import org.eclipse.php.internal.ui.PHPUiPlugin;
import org.eclipse.php.internal.ui.editor.PHPStructuredTextViewer;
import org.eclipse.php.internal.ui.editor.SemanticHighlighting;
import org.eclipse.php.internal.ui.editor.SemanticHighlightingManager;
import org.eclipse.php.internal.ui.editor.SemanticHighlightingManager.HighlightedRange;
import org.eclipse.php.internal.ui.editor.SemanticHighlightings;
import org.eclipse.php.internal.ui.editor.configuration.PHPStructuredTextViewerConfiguration;
import org.eclipse.php.internal.ui.editor.highlighter.LineStyleProviderForPHP;
import org.eclipse.php.internal.ui.util.ScrolledPageContent;
import org.eclipse.php.internal.ui.wizards.fields.LayoutUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore;
import org.eclipse.wst.sse.ui.internal.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.wst.sse.ui.internal.preferences.ui.ColorHelper;

/**
 * Configures Java Editor hover preferences.
 *
 * @since 2.1
 */
class PHPEditorColoringConfigurationBlock extends AbstractConfigurationBlock {

	/**
	 * Item in the highlighting color list.
	 *
	 * @since 3.0
	 */
	private static class HighlightingColorListItem {
		/** Display name */
		private String fDisplayName;
		/** Color preference key */
		private String fColorKey;
		/** Bold preference key */
		private String fBoldKey;
		/** Italic preference key */
		private String fItalicKey;
		/**
		 * Strikethrough preference key.
		 *
		 * @since 3.1
		 */
		private String fStrikethroughKey;
		/**
		 * Underline preference key.
		 *
		 * @since 3.1
		 */
		private String fUnderlineKey;
		/** Enablement preference key */
		private final String fEnableKey;

		/**
		 * Initialize the item with the given values.
		 *
		 * @param displayName
		 *            the display name
		 * @param colorKey
		 *            the color preference key
		 * @param boldKey
		 *            the bold preference key
		 * @param italicKey
		 *            the italic preference key
		 * @param strikethroughKey
		 *            the strikethrough preference key
		 * @param underlineKey
		 *            the underline preference key
		 */
		public HighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey,
				String strikethroughKey, String underlineKey, String enableKey) {
			fDisplayName = displayName;
			fColorKey = colorKey;
			fBoldKey = boldKey;
			fItalicKey = italicKey;
			fStrikethroughKey = strikethroughKey;
			fUnderlineKey = underlineKey;
			fEnableKey = enableKey;
		}

		/**
		 * @return the bold preference key
		 */
		public String getBoldKey() {
			return fBoldKey;
		}

		/**
		 * @return the bold preference key
		 */
		public String getItalicKey() {
			return fItalicKey;
		}

		/**
		 * @return the strikethrough preference key
		 * @since 3.1
		 */
		public String getStrikethroughKey() {
			return fStrikethroughKey;
		}

		/**
		 * @return the underline preference key
		 * @since 3.1
		 */
		public String getUnderlineKey() {
			return fUnderlineKey;
		}

		/**
		 * @return the color preference key
		 */
		public String getColorKey() {
			return fColorKey;
		}

		/**
		 * @return the display name
		 */
		public String getDisplayName() {
			return fDisplayName;
		}

		/**
		 * @return the enablement preference key
		 */
		public String getEnableKey() {
			return fEnableKey;
		}
	}

	private static class SemanticHighlightingColorListItem extends HighlightingColorListItem {

		public SemanticHighlightingColorListItem(String displayName, String colorKey, String boldKey, String italicKey,
				String strikethroughKey, String underlineKey, String enableKey) {
			super(displayName, colorKey, boldKey, italicKey, strikethroughKey, underlineKey, enableKey);
		}
	}

	/**
	 * Color list label provider.
	 *
	 * @since 3.0
	 */
	private class ColorListLabelProvider extends LabelProvider {
		/*
		 * @see
		 * org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			if (element instanceof String)
				return (String) element;
			return ((HighlightingColorListItem) element).getDisplayName();
		}
	}

	/**
	 * Color list content provider.
	 *
	 * @since 3.0
	 */
	private class ColorListContentProvider implements ITreeContentProvider {

		/*
		 * @see
		 * org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java
		 * .lang.Object)
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			return new String[] { fPHPCategory, fPHPDocCategory, fCommentsCategory };
		}

		/*
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		@Override
		public void dispose() {
		}

		/*
		 * @see
		 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.
		 * jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof String) {
				String entry = (String) parentElement;
				if (fPHPCategory.equals(entry))
					return fListModel.subList(0, fListModel.size()).toArray();
				if (fPHPDocCategory.equals(entry))
					return fListModel.subList(0, 4).toArray();
				if (fCommentsCategory.equals(entry))
					return fListModel.subList(4, 7).toArray();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof String)
				return null;
			int index = fListModel.indexOf(element);
			if (index < 4)
				return fPHPDocCategory;
			if (index >= 7)
				return fPHPCategory;
			return fCommentsCategory;
		}

		@Override
		public boolean hasChildren(Object element) {
			return element instanceof String;
		}
	}

	private static final String BOLD = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_BOLD_SUFFIX;
	/**
	 * Preference key suffix for italic preferences.
	 *
	 * @since 3.0
	 */
	private static final String ITALIC = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ITALIC_SUFFIX;
	/**
	 * Preference key suffix for strikethrough preferences.
	 *
	 * @since 3.1
	 */
	private static final String STRIKETHROUGH = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_STRIKETHROUGH_SUFFIX;
	/**
	 * Preference key suffix for underline preferences.
	 *
	 * @since 3.1
	 */
	private static final String UNDERLINE = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_UNDERLINE_SUFFIX;
	/**
	 * Preference key suffix for enabled preferences.
	 *
	 * @since 3.1
	 */
	private static final String ENABLED = PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;

	// private static final String COMPILER_TASK_TAGS =
	// JavaCore.COMPILER_TASK_TAGS;
	/**
	 * The keys of the overlay store.
	 */
	/**
	 * The keys of the overlay store.
	 */
	private final String[][] fSyntaxColorListModel = new String[][] {
			{ PHPUIMessages.ColorPage_Phpdoc, PreferenceConstants.EDITOR_PHPDOC_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_javaDocHtmlTags,
			// PreferenceConstants.EDITOR_JAVADOC_TAG_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_javaDocLinks,
			// PreferenceConstants.EDITOR_JAVADOC_LINKS_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_javaDocOthers,
			// PreferenceConstants.EDITOR_JAVADOC_DEFAULT_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_multiLineComment,
			// PreferenceConstants.EDITOR_MULTI_LINE_COMMENT_COLOR },
			{ PreferencesMessages.JavaEditorPreferencePage_singleLineComment,
					PreferenceConstants.EDITOR_LINE_COMMENT_COLOR },
			// {
			// PreferencesMessages.JavaEditorPreferencePage_javaCommentTaskTags,
			// PreferenceConstants.EDITOR_TASK_TAG_COLOR },
			{ PreferencesMessages.JavaEditorPreferencePage_keywords, PreferenceConstants.EDITOR_KEYWORD_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_returnKeyword,
			// PreferenceConstants.EDITOR_JAVA_KEYWORD_RETURN_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_operators,
			// PreferenceConstants.EDITOR_JAVA_OPERATOR_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_brackets,
			// PreferenceConstants.EDITOR_JAVA_BRACKET_COLOR },
			{ PreferencesMessages.JavaEditorPreferencePage_strings, PreferenceConstants.EDITOR_STRING_COLOR },
			// { PreferencesMessages.JavaEditorPreferencePage_others,
			// PreferenceConstants.EDITOR_JAVA_DEFAULT_COLOR },
	};

	private final String fPHPCategory = PreferencesMessages.JavaEditorPreferencePage_coloring_category_java;
	private final String fPHPDocCategory = PreferencesMessages.JavaEditorPreferencePage_coloring_category_javadoc;
	private final String fCommentsCategory = PreferencesMessages.JavaEditorPreferencePage_coloring_category_comments;

	private ColorSelector fSyntaxForegroundColorEditor;
	private Label fColorEditorLabel;
	private Button fBoldCheckBox;
	private Button fEnableCheckbox;
	/**
	 * Check box for italic preference.
	 *
	 * @since 3.0
	 */
	private Button fItalicCheckBox;
	/**
	 * Check box for strikethrough preference.
	 *
	 * @since 3.1
	 */
	private Button fStrikethroughCheckBox;
	/**
	 * Check box for underline preference.
	 *
	 * @since 3.1
	 */
	private Button fUnderlineCheckBox;
	/**
	 * Highlighting color list
	 *
	 * @since 3.0
	 */
	private final java.util.List<HighlightingColorListItem> fListModel = new ArrayList<>();
	/**
	 * Highlighting color tree viewer
	 *
	 * @since 3.0
	 */
	private TreeViewer fTreeViewer;
	/**
	 * Semantic highlighting manager
	 *
	 * @since 3.0
	 */
	private SemanticHighlightingManager fSemanticHighlightingManager;
	/**
	 * The previewer.
	 *
	 * @since 3.0
	 */
	private PHPStructuredTextViewer fPreviewViewer;
	/**
	 * The color manager.
	 *
	 * @since 3.1
	 */
	private ColorManager fColorManager;
	/**
	 * The font metrics.
	 *
	 * @since 3.1
	 */
	private FontMetrics fFontMetrics;

	private final LineStyleProviderForPHP fStyleProvider;

	public PHPEditorColoringConfigurationBlock(OverlayPreferenceStore store) {
		super(store);

		fColorManager = new ColorManager(false);

		for (int i = 0, n = fSyntaxColorListModel.length; i < n; i++)
			fListModel.add(new HighlightingColorListItem(fSyntaxColorListModel[i][0], fSyntaxColorListModel[i][1],
					PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + fSyntaxColorListModel[i][1] + BOLD,
					PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + fSyntaxColorListModel[i][1] + ITALIC,
					PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + fSyntaxColorListModel[i][1]
							+ STRIKETHROUGH,
					PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + fSyntaxColorListModel[i][1] + UNDERLINE,
					PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + fSyntaxColorListModel[i][1] + ENABLED));

		SemanticHighlighting[] semanticHighlightings = SemanticHighlightings.getSemanticHighlightings();
		for (int i = 0, n = semanticHighlightings.length; i < n; i++)
			fListModel.add(new SemanticHighlightingColorListItem(semanticHighlightings[i].getDisplayName(),
					SemanticHighlightings.getColorPreferenceKey(semanticHighlightings[i]),
					SemanticHighlightings.getBoldPreferenceKey(semanticHighlightings[i]),
					SemanticHighlightings.getItalicPreferenceKey(semanticHighlightings[i]),
					SemanticHighlightings.getStrikethroughPreferenceKey(semanticHighlightings[i]),
					SemanticHighlightings.getUnderlinePreferenceKey(semanticHighlightings[i]),
					SemanticHighlightings.getEnabledPreferenceKey(semanticHighlightings[i])));

		store.addKeys(createOverlayStoreKeys());
		store.load();

		fStyleProvider = new LineStyleProviderForPHP();
		fStyleProvider.setColorPreferences(store);
		fStyleProvider.loadColors();
	}

	private OverlayPreferenceStore.OverlayKey[] createOverlayStoreKeys() {

		ArrayList<OverlayKey> overlayKeys = new ArrayList<>();

		for (int i = 0, n = fListModel.size(); i < n; i++) {
			HighlightingColorListItem item = fListModel.get(i);
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.STRING, item.getColorKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getBoldKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getItalicKey()));
			overlayKeys.add(
					new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getStrikethroughKey()));
			overlayKeys
					.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getUnderlineKey()));
			overlayKeys.add(new OverlayPreferenceStore.OverlayKey(OverlayPreferenceStore.BOOLEAN, item.getEnableKey()));
		}

		OverlayPreferenceStore.OverlayKey[] keys = new OverlayPreferenceStore.OverlayKey[overlayKeys.size()];
		overlayKeys.toArray(keys);
		return keys;
	}

	/**
	 * Creates page for hover preferences.
	 *
	 * @param parent
	 *            the parent composite
	 * @return the control for the preference page
	 */
	@Override
	public Control createControl(Composite parent) {
		initializeDialogUnits(parent);

		ScrolledPageContent scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolled.setExpandHorizontal(true);
		scrolled.setExpandVertical(true);

		Control control = createSyntaxPage(scrolled);

		scrolled.setContent(control);
		final Point size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		scrolled.setMinSize(size.x, size.y);

		return scrolled;
	}

	/**
	 * Returns the number of pixels corresponding to the width of the given
	 * number of characters.
	 * <p>
	 * This method may only be called after <code>initializeDialogUnits</code>
	 * has been called.
	 * </p>
	 * <p>
	 * Clients may call this framework method, but should not override it.
	 * </p>
	 *
	 * @param chars
	 *            the number of characters
	 * @return the number of pixels
	 */
	private int convertWidthInCharsToPixels(int chars) {
		// test for failure to initialize for backward compatibility
		if (fFontMetrics == null)
			return 0;
		return Dialog.convertWidthInCharsToPixels(fFontMetrics, chars);
	}

	/**
	 * Returns the number of pixels corresponding to the height of the given
	 * number of characters.
	 * <p>
	 * This method may only be called after <code>initializeDialogUnits</code>
	 * has been called.
	 * </p>
	 * <p>
	 * Clients may call this framework method, but should not override it.
	 * </p>
	 *
	 * @param chars
	 *            the number of characters
	 * @return the number of pixels
	 */
	private int convertHeightInCharsToPixels(int chars) {
		// test for failure to initialize for backward compatibility
		if (fFontMetrics == null)
			return 0;
		return Dialog.convertHeightInCharsToPixels(fFontMetrics, chars);
	}

	@Override
	public void initialize() {
		super.initialize();

		fTreeViewer.setInput(fListModel);
		fTreeViewer.setSelection(new StructuredSelection(fPHPCategory));
	}

	@Override
	public void performDefaults() {
		super.performDefaults();

		handleSyntaxColorListSelection();

		uninstallSemanticHighlighting();
		installSemanticHighlighting();

		fPreviewViewer.invalidateTextPresentation();
	}

	/*
	 * @see
	 * org.eclipse.jdt.internal.ui.preferences.IPreferenceConfigurationBlock#
	 * dispose()
	 */
	@Override
	public void dispose() {
		uninstallSemanticHighlighting();
		fColorManager.dispose();

		super.dispose();
	}

	private void handleSyntaxColorListSelection() {
		HighlightingColorListItem item = getHighlightingColorListItem();
		if (item == null) {
			fEnableCheckbox.setEnabled(false);
			fSyntaxForegroundColorEditor.getButton().setEnabled(false);
			fColorEditorLabel.setEnabled(false);
			fBoldCheckBox.setEnabled(false);
			fItalicCheckBox.setEnabled(false);
			fStrikethroughCheckBox.setEnabled(false);
			fUnderlineCheckBox.setEnabled(false);
			return;
		}

		if (item instanceof SemanticHighlightingColorListItem) {
			RGB rgb = PreferenceConverter.getColor(getPreferenceStore(), item.getColorKey());
			fSyntaxForegroundColorEditor.setColorValue(rgb);
			fBoldCheckBox.setSelection(getPreferenceStore().getBoolean(item.getBoldKey()));
			fItalicCheckBox.setSelection(getPreferenceStore().getBoolean(item.getItalicKey()));
			fStrikethroughCheckBox.setSelection(getPreferenceStore().getBoolean(item.getStrikethroughKey()));
			fUnderlineCheckBox.setSelection(getPreferenceStore().getBoolean(item.getUnderlineKey()));

			fEnableCheckbox.setEnabled(true);
			boolean enable = getPreferenceStore().getBoolean(((SemanticHighlightingColorListItem) item).getEnableKey());
			fEnableCheckbox.setSelection(enable);
			fSyntaxForegroundColorEditor.getButton().setEnabled(enable);
			fColorEditorLabel.setEnabled(enable);
			fBoldCheckBox.setEnabled(enable);
			fItalicCheckBox.setEnabled(enable);
			fStrikethroughCheckBox.setEnabled(enable);
			fUnderlineCheckBox.setEnabled(enable);
		} else {
			String prefString = getPreferenceStore().getString(item.getColorKey());
			String[] stylePrefs = ColorHelper.unpackStylePreferences(prefString);

			RGB color = StringConverter.asRGB(stylePrefs[0], null);
			fSyntaxForegroundColorEditor.setColorValue(color);
			fBoldCheckBox.setSelection(Boolean.valueOf(stylePrefs[2]).booleanValue());
			fItalicCheckBox.setSelection(Boolean.valueOf(stylePrefs[3]).booleanValue());
			fStrikethroughCheckBox.setSelection(Boolean.valueOf(stylePrefs[4]).booleanValue());
			fUnderlineCheckBox.setSelection(Boolean.valueOf(stylePrefs[5]).booleanValue());

			fSyntaxForegroundColorEditor.getButton().setEnabled(true);
			fColorEditorLabel.setEnabled(true);
			fBoldCheckBox.setEnabled(true);
			fItalicCheckBox.setEnabled(true);
			fStrikethroughCheckBox.setEnabled(true);
			fUnderlineCheckBox.setEnabled(true);
			fEnableCheckbox.setEnabled(false);
			fEnableCheckbox.setSelection(true);
		}
	}

	private Control createSyntaxPage(final Composite parent) {

		Composite colorComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		colorComposite.setLayout(layout);

		Link link = new Link(colorComposite, SWT.NONE);
		link.setText(PreferencesMessages.JavaEditorColoringConfigurationBlock_link);
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if ("org.eclipse.ui.preferencePages.GeneralTextEditor".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(parent.getShell(), e.text, null, null);
				else if ("org.eclipse.ui.preferencePages.ColorsAndFonts".equals(e.text)) //$NON-NLS-1$
					PreferencesUtil.createPreferenceDialogOn(parent.getShell(), e.text, null,
							"selectFont:org.eclipse.wst.sse.ui.textfont"); //$NON-NLS-1$
			}
		});

		GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		gridData.widthHint = 150; // only expand further if anyone else requires
									// it
		gridData.horizontalSpan = 2;
		link.setLayoutData(gridData);

		addFiller(colorComposite, 1);

		Label label;
		label = new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.JavaEditorPreferencePage_coloring_element);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite editorComposite = new Composite(colorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		editorComposite.setLayout(layout);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = convertHeightInCharsToPixels(7);
		editorComposite.setLayoutData(gd);

		fTreeViewer = new TreeViewer(editorComposite, SWT.SINGLE | SWT.BORDER);
		fTreeViewer.setLabelProvider(new ColorListLabelProvider());
		fTreeViewer.setContentProvider(new ColorListContentProvider());
		fTreeViewer.setComparator(new ViewerComparator() {
			@Override
			public int category(Object element) {
				// don't sort the top level categories
				if (fPHPCategory.equals(element))
					return 0;
				if (fPHPDocCategory.equals(element))
					return 1;
				if (fCommentsCategory.equals(element))
					return 2;
				// to sort semantic settings after partition based ones:
				// if (element instanceof SemanticHighlightingColorListItem)
				// return 1;
				return 0;
			}
		});
		gd = new GridData(SWT.BEGINNING, SWT.FILL, false, true);
		gd.heightHint = convertHeightInCharsToPixels(7);
		int maxWidth = 0;
		for (Iterator<HighlightingColorListItem> it = fListModel.iterator(); it.hasNext();) {
			HighlightingColorListItem item = it.next();
			maxWidth = Math.max(maxWidth, convertWidthInCharsToPixels(item.getDisplayName().length()));
		}
		ScrollBar vBar = ((Scrollable) fTreeViewer.getControl()).getVerticalBar();
		if (vBar != null)
			maxWidth += vBar.getSize().x * 3; // scrollbars and tree indentation
												// guess
		gd.widthHint = maxWidth;

		fTreeViewer.getControl().setLayoutData(gd);
		installDoubleClickListener();

		Composite stylesComposite = new Composite(editorComposite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 2;
		stylesComposite.setLayout(layout);
		stylesComposite.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false));

		fEnableCheckbox = new Button(stylesComposite, SWT.CHECK);
		fEnableCheckbox.setText(PreferencesMessages.JavaEditorPreferencePage_enable);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalAlignment = GridData.BEGINNING;
		gd.horizontalSpan = 2;
		fEnableCheckbox.setLayoutData(gd);

		fColorEditorLabel = new Label(stylesComposite, SWT.LEFT);
		fColorEditorLabel.setText(PreferencesMessages.JavaEditorPreferencePage_color);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = LayoutUtil.getIndent();
		fColorEditorLabel.setLayoutData(gd);

		fSyntaxForegroundColorEditor = new ColorSelector(stylesComposite);
		Button foregroundColorButton = fSyntaxForegroundColorEditor.getButton();
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		foregroundColorButton.setLayoutData(gd);

		fBoldCheckBox = new Button(stylesComposite, SWT.CHECK);
		fBoldCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_bold);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = LayoutUtil.getIndent();
		gd.horizontalSpan = 2;
		fBoldCheckBox.setLayoutData(gd);

		fItalicCheckBox = new Button(stylesComposite, SWT.CHECK);
		fItalicCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_italic);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = LayoutUtil.getIndent();
		gd.horizontalSpan = 2;
		fItalicCheckBox.setLayoutData(gd);

		fStrikethroughCheckBox = new Button(stylesComposite, SWT.CHECK);
		fStrikethroughCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_strikethrough);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = LayoutUtil.getIndent();
		gd.horizontalSpan = 2;
		fStrikethroughCheckBox.setLayoutData(gd);

		fUnderlineCheckBox = new Button(stylesComposite, SWT.CHECK);
		fUnderlineCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_underline);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent = LayoutUtil.getIndent();
		gd.horizontalSpan = 2;
		fUnderlineCheckBox.setLayoutData(gd);

		label = new Label(colorComposite, SWT.LEFT);
		label.setText(PreferencesMessages.JavaEditorPreferencePage_preview);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Control previewer = createPreviewer(colorComposite);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = convertWidthInCharsToPixels(20);
		previewer.setLayoutData(gd);

		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				handleSyntaxColorListSelection();
			}
		});

		foregroundColorButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				PreferenceConverter.setValue(getPreferenceStore(), item.getColorKey(),
						fSyntaxForegroundColorEditor.getColorValue());
				fStyleProvider.loadColors();
			}
		});

		fBoldCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getPreferenceStore().setValue(item.getBoldKey(), fBoldCheckBox.getSelection());
				fStyleProvider.loadColors();
			}
		});

		fItalicCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getPreferenceStore().setValue(item.getItalicKey(), fItalicCheckBox.getSelection());
				fStyleProvider.loadColors();
			}
		});
		fStrikethroughCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getPreferenceStore().setValue(item.getStrikethroughKey(), fStrikethroughCheckBox.getSelection());
				fStyleProvider.loadColors();
			}
		});

		fUnderlineCheckBox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				getPreferenceStore().setValue(item.getUnderlineKey(), fUnderlineCheckBox.getSelection());
				fStyleProvider.loadColors();
			}
		});

		fEnableCheckbox.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}

			@Override
			public void widgetSelected(SelectionEvent e) {
				HighlightingColorListItem item = getHighlightingColorListItem();
				if (item instanceof SemanticHighlightingColorListItem) {
					boolean enable = fEnableCheckbox.getSelection();
					getPreferenceStore().setValue(((SemanticHighlightingColorListItem) item).getEnableKey(), enable);
					fEnableCheckbox.setSelection(enable);
					fSyntaxForegroundColorEditor.getButton().setEnabled(enable);
					fColorEditorLabel.setEnabled(enable);
					fBoldCheckBox.setEnabled(enable);
					fItalicCheckBox.setEnabled(enable);
					fStrikethroughCheckBox.setEnabled(enable);
					fUnderlineCheckBox.setEnabled(enable);
					uninstallSemanticHighlighting();
					installSemanticHighlighting();
					fStyleProvider.loadColors();
				}
			}
		});

		colorComposite.layout();

		return colorComposite;
	}

	/**
	 * Installs a double-click listener which allows to expand and collapse tree
	 * items.
	 *
	 * @since 3.4
	 */
	private void installDoubleClickListener() {
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			/*
			 * @see
			 * org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.
			 * eclipse.jface.viewers.DoubleClickEvent)
			 */
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection s = (IStructuredSelection) event.getSelection();
				Object element = s.getFirstElement();
				if (fTreeViewer.isExpandable(element))
					fTreeViewer.setExpandedState(element, !fTreeViewer.getExpandedState(element));
			}
		});
	}

	private void addFiller(Composite composite, int horizontalSpan) {
		PixelConverter pixelConverter = new PixelConverter(composite);
		Label filler = new Label(composite, SWT.LEFT);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = horizontalSpan;
		gd.heightHint = pixelConverter.convertHeightInCharsToPixels(1) / 2;
		filler.setLayoutData(gd);
	}

	private Control createPreviewer(Composite parent) {

		IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
		IPreferenceStore store = new ChainedPreferenceStore(
				new IPreferenceStore[] { getPreferenceStore(), generalTextStore });
		fPreviewViewer = new PHPStructuredTextViewer(parent, null, null, false, SWT.H_SCROLL | SWT.BORDER);
		PHPStructuredTextViewerConfiguration configuration = new PHPStructuredTextViewerConfiguration();
		fPreviewViewer.configure(configuration);
		Font font = JFaceResources.getFont("org.eclipse.wst.sse.ui.textfont");
		fPreviewViewer.getTextWidget().setFont(font);
		new PHPSourcePreviewerUpdater(fPreviewViewer, configuration, store);

		fPreviewViewer.setEditable(false);
		Cursor arrowCursor = fPreviewViewer.getTextWidget().getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
		fPreviewViewer.getTextWidget().setCursor(arrowCursor);

		String content = PHPUIMessages.ColorPage_CodeExample_0; // $NON-NLS-1$
		IStructuredDocument document = StructuredModelManager.getModelManager()
				.createStructuredDocumentFor(ContentTypeIdForPHP.ContentTypeID_PHP);
		document.set(content);
		// PHPUiPlugin.getDefault().getTextTools().setupJavaDocumentPartitioner(document,
		// IJavaPartitions.JAVA_PARTITIONING);
		fPreviewViewer.setDocument(document);

		installSemanticHighlighting();

		return fPreviewViewer.getControl();
	}

	/**
	 * Install Semantic Highlighting on the previewer
	 *
	 * @since 3.0
	 */
	private void installSemanticHighlighting() {
		if (fSemanticHighlightingManager == null) {
			fSemanticHighlightingManager = new SemanticHighlightingManager();
			fSemanticHighlightingManager.install(fPreviewViewer, fColorManager, getPreferenceStore(),
					createPreviewerRanges());
		}
	}

	/**
	 * Uninstall Semantic Highlighting from the previewer
	 *
	 * @since 3.0
	 */
	private void uninstallSemanticHighlighting() {
		if (fSemanticHighlightingManager != null) {
			fSemanticHighlightingManager.uninstall();
			fSemanticHighlightingManager = null;
		}
	}

	/**
	 * Create the hard coded previewer ranges
	 *
	 * @return the hard coded previewer ranges
	 * @since 3.0
	 */
	private SemanticHighlightingManager.HighlightedRange[][] createPreviewerRanges() {
		return new SemanticHighlightingManager.HighlightedRange[][] {
				// { createHighlightedRange( 6, 13, 9,
				// SemanticHighlightings.DEPRECATED_MEMBER),
				// createHighlightedRange( 6, 13, 9,
				// SemanticHighlightings.CLASS), },
				// { createHighlightedRange( 6, 23, 1,
				// SemanticHighlightings.TYPE_VARIABLE), createHighlightedRange(
				// 6, 23, 1, SemanticHighlightings.TYPE_ARGUMENT), },
				{ createHighlightedRange(6, 34, 8, SemanticHighlightings.CLASS) },
				// { createHighlightedRange( 6, 54, 13,
				// SemanticHighlightings.INTERFACE) },
				// { createHighlightedRange( 6, 68, 6,
				// SemanticHighlightings.TYPE_ARGUMENT), createHighlightedRange(
				// 6, 68, 6, SemanticHighlightings.CLASS) },
				// { createHighlightedRange( 7, 6, 5,
				// SemanticHighlightings.ENUM), },
				// { createHighlightedRange( 7, 14, 3,
				// SemanticHighlightings.STATIC_FINAL_FIELD),
				// createHighlightedRange( 7, 14, 3,
				// SemanticHighlightings.STATIC_FIELD),
				// createHighlightedRange(7, 14, 3, SemanticHighlightings.FIELD)
				// },
				// { createHighlightedRange( 7, 19, 5,
				// SemanticHighlightings.STATIC_FINAL_FIELD),
				// createHighlightedRange( 7, 19, 5,
				// SemanticHighlightings.STATIC_FIELD),
				// createHighlightedRange(7, 19, 5, SemanticHighlightings.FIELD)
				// },
				// { createHighlightedRange( 7, 26, 4,
				// SemanticHighlightings.STATIC_FINAL_FIELD),
				// createHighlightedRange( 7, 26, 4,
				// SemanticHighlightings.STATIC_FIELD),
				// createHighlightedRange(7, 26, 4, SemanticHighlightings.FIELD)
				// },
				// { createHighlightedRange( 9, 8, 6,
				// SemanticHighlightings.CLASS), },
				// { createHighlightedRange( 9, 15, 11,
				// SemanticHighlightings.STATIC_FIELD), createHighlightedRange(
				// 9, 15, 11, SemanticHighlightings.FIELD) },
				// { createHighlightedRange(11, 9, 1,
				// SemanticHighlightings.TYPE_VARIABLE) },
				// { createHighlightedRange(11, 11, 5,
				// SemanticHighlightings.FIELD) },
				// { createHighlightedRange(12, 9, 17,
				// SemanticHighlightings.ABSTRACT_CLASS),
				// createHighlightedRange(12, 9, 17,
				// SemanticHighlightings.CLASS) },
				// { createHighlightedRange(12, 27, 6,
				// SemanticHighlightings.FIELD) },
				// { createHighlightedRange(14, 2, 16,
				// SemanticHighlightings.ANNOTATION) },
				// { createHighlightedRange(14, 19, 5,
				// SemanticHighlightings.ANNOTATION_ELEMENT_REFERENCE) },
				// { createHighlightedRange(15, 12, 3,
				// SemanticHighlightings.METHOD_DECLARATION),
				// createHighlightedRange(15, 12, 3,
				// SemanticHighlightings.METHOD) },
				// { createHighlightedRange(15, 16, 7,
				// SemanticHighlightings.CLASS) },
				// { createHighlightedRange(15, 24, 9,
				// SemanticHighlightings.PARAMETER_VARIABLE),
				// createHighlightedRange(15, 24, 9,
				// SemanticHighlightings.LOCAL_VARIABLE_DECLARATION),
				// createHighlightedRange(15, 24, 9,
				// SemanticHighlightings.LOCAL_VARIABLE) },
				// { createHighlightedRange(16, 2, 14,
				// SemanticHighlightings.ABSTRACT_METHOD_INVOCATION),
				// createHighlightedRange(16, 2, 14,
				// SemanticHighlightings.METHOD) },
				// { createHighlightedRange(16, 17, 14,
				// SemanticHighlightings.INHERITED_FIELD),
				// createHighlightedRange(16, 17, 14,
				// SemanticHighlightings.FIELD) },
				// { createHighlightedRange(17, 6, 5,
				// SemanticHighlightings.LOCAL_VARIABLE_DECLARATION),
				// createHighlightedRange(17, 6, 5,
				// SemanticHighlightings.LOCAL_VARIABLE) },
				// { createHighlightedRange(17, 13, 2,
				// SemanticHighlightings.NUMBER) },
				// { createHighlightedRange(17, 16, 8,
				// SemanticHighlightings.INHERITED_METHOD_INVOCATION),
				// createHighlightedRange(17, 16, 8,
				// SemanticHighlightings.METHOD) },
				// { createHighlightedRange(18, 2, 12,
				// SemanticHighlightings.STATIC_METHOD_INVOCATION),
				// createHighlightedRange(18, 2, 12,
				// SemanticHighlightings.METHOD) },
				// { createHighlightedRange(19, 9, 3,
				// SemanticHighlightings.METHOD) },
				// { createHighlightedRange(19, 13, 5,
				// SemanticHighlightings.LOCAL_VARIABLE) },
				// { createHighlightedRange(19, 22, 9,
				// SemanticHighlightings.AUTOBOXING), createHighlightedRange(19,
				// 22, 9, SemanticHighlightings.PARAMETER_VARIABLE),
				// createHighlightedRange(19, 22, 9,
				// SemanticHighlightings.LOCAL_VARIABLE) },
		};
	}

	/**
	 * Create a highlighted range on the previewers document with the given
	 * line, column, length and key.
	 *
	 * @param line
	 *            the line
	 * @param column
	 *            the column
	 * @param length
	 *            the length
	 * @param key
	 *            the key
	 * @return the highlighted range
	 * @since 3.0
	 */
	private HighlightedRange createHighlightedRange(int line, int column, int length, String key) {
		try {
			IDocument document = fPreviewViewer.getDocument();
			int offset = document.getLineOffset(line) + column;
			return new HighlightedRange(offset, length, key);
		} catch (BadLocationException x) {
			PHPUiPlugin.log(x);
		}
		return null;
	}

	/**
	 * Returns the current highlighting color list item.
	 *
	 * @return the current highlighting color list item
	 * @since 3.0
	 */
	private HighlightingColorListItem getHighlightingColorListItem() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		Object element = selection.getFirstElement();
		if (element instanceof String)
			return null;
		return (HighlightingColorListItem) element;
	}

	/**
	 * Initializes the computation of horizontal and vertical dialog units based
	 * on the size of current font.
	 * <p>
	 * This method must be called before any of the dialog unit based conversion
	 * methods are called.
	 * </p>
	 *
	 * @param testControl
	 *            a control from which to obtain the current font
	 */
	private void initializeDialogUnits(Control testControl) {
		// Compute and store a font metric
		GC gc = new GC(testControl);
		gc.setFont(JFaceResources.getDialogFont());
		fFontMetrics = gc.getFontMetrics();
		gc.dispose();
	}
}
