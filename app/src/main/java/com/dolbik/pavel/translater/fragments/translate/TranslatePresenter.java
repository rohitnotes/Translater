package com.dolbik.pavel.translater.fragments.translate;

import android.text.TextUtils;
import android.util.Pair;

import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.dolbik.pavel.translater.TApplication;
import com.dolbik.pavel.translater.db.DataRepository;
import com.dolbik.pavel.translater.db.Repository;
import com.dolbik.pavel.translater.events.ChangeLangEvent;
import com.dolbik.pavel.translater.model.Language;
import com.dolbik.pavel.translater.model.Translate;
import com.dolbik.pavel.translater.rest.ErrorHandler;
import com.dolbik.pavel.translater.utils.Constants;

import net.grandcentrix.tray.AppPreferences;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import rx.Observable;
import rx.SingleSubscriber;
import rx.Subscriber;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;


@InjectViewState
public class TranslatePresenter extends MvpPresenter<TranslateView> {

    private EventBus              bus;
    private Repository            repository;
    private TApplication          application;
    private AppPreferences        pref;
    private CompositeSubscription compositeSbs;

    /** Текущее направление перевода. <br>
     *  Current direction of translation. */
    private Pair<Language, Language> languagePair;

    /** Направление перевода. <br>
     *  Direction of translation. */
    private String translateDirection;

    /** Текущий текст который переводится. <br>
     *  The current text is translated. */
    private String translateText;

    private Subscription translateSbs;


    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        bus          = EventBus.getDefault();
        repository   = new DataRepository();
        compositeSbs = new CompositeSubscription();
        bus.register(this);

        prepareInstallLangsFromJson();
    }


    /** При первом запуске, заполняем БД и отображаем направление перевода. <br>
     *  At the first start, fill out the database and display the direction of the translation. */
    private void prepareInstallLangsFromJson() {
        Subscription sbs = repository.preInstallLangs()
                .subscribe(new Subscriber<Pair<Language, Language>>() {
                    @Override
                    public void onNext(Pair<Language, Language> pair) {
                        languagePair = pair;
                        getViewState().showToolbarView();
                        updateTranslationDirection();
                        translateLangForCurrentLocale();
                    }

                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }
        });
        compositeSbs.add(sbs);
    }


    /** Переводим языки в соответствии с текущей локалью. <br>
     *  Translate languages according to the current locale. */
    private void translateLangForCurrentLocale() {
        if (getApplication().isConnected()) {
            Subscription sbs = repository.getAllLangs()
                    .subscribe(new Subscriber<Pair<Language, Language>>() {
                        @Override
                        public void onNext(Pair<Language, Language> pair) {
                            languagePair = pair;
                            updateTranslationDirection();
                        }

                        @Override
                        public void onCompleted() {}

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            getViewState().showSnakeBar(ErrorHandler.getInstance().getErrorMessage(e));
                        }
                    });
            compositeSbs.add(sbs);
        }
    }


    void translateText(String text, boolean forceTranslate) {
        if (TextUtils.isEmpty(text)) {
            clear();
        } else {
            getViewState().showCleanBtn();
            if (getApplication().isConnected()) {
                unsubscribeTranslateSbs();
                if (translateText == null || !translateText.equals(text) || forceTranslate) {
                    translateText = text;
                    getViewState().showViewStub(TranslateFragmentState.SHOW_PROGRESS, null);
                    translateSbs = repository.getTranslate(text, translateDirection)
                            .subscribe(new SingleSubscriber<Translate>() {
                                @Override
                                public void onSuccess(Translate value) {
                                    getViewState().showHideFavoriteBtn(true);
                                    getViewState().showViewStub(
                                            TranslateFragmentState.SHOW_TRANSLATE, value.getText().get(0));
                                }

                                @Override
                                public void onError(Throwable error) {
                                    error.printStackTrace();
                                    getViewState().showHideFavoriteBtn(false);
                                    getViewState().showViewStub(TranslateFragmentState.IDLE, null);
                                    getViewState().showSnakeBar(ErrorHandler.getInstance().getErrorMessage(error));
                                }
                            });
                }
            } else {
                getViewState().showHideFavoriteBtn(false);
                getViewState().showViewStub(TranslateFragmentState.SHOW_ERROR, null);
            }
        }
    }


    /** Направление перевода. Параметр в запросе API. <br>
     *  Direction of translation. Parameter in the API request. */
    private void setTranslateDirection() {
        if (languagePair != null) {
            translateDirection = new StringBuilder()
                    .append(languagePair.first.getCode())
                    .append("-")
                    .append(languagePair.second.getCode()).toString();
        }
    }


    /** Обновляем направление перевода в UI. <br>
     *  Update the direction of the translation in UI. */
    private void updateTranslationDirection() {
        setTranslateDirection();
        getViewState().updateTranslateDirection(languagePair.first.getName(), languagePair.second.getName());
    }


    /** Обновляем значения хранящиеся в preferences. <br>
     *  Update the values stored in preferences. */
    private void updateStorePair() {
        Observable.fromCallable(() -> {
            getPref().put(Constants.DIRC_FROM_CODE, languagePair.first.getCode());
            getPref().put(Constants.DIRC_FROM_NAME, languagePair.first.getName());
            getPref().put(Constants.DIRC_TO_CODE,   languagePair.second.getCode());
            getPref().put(Constants.DIRC_TO_NAME,   languagePair.second.getName());
            return true;
        }).subscribeOn(Schedulers.io()).subscribe();
    }


    //Посылается из ChangeLanguagePresenter.
    //It is sent from ChangeLanguagePresenter.
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(ChangeLangEvent.From event) {
        bus.removeStickyEvent(event);
        Language language     = event.getLanguage();
        Language tempLanguage = languagePair.second;
        if (language.getCode().equals(tempLanguage.getCode())) {
            tempLanguage = languagePair.first;
        }
        languagePair = new Pair<>(language, tempLanguage);
        changeLangUpdate();
    }


    //Посылается из ChangeLanguagePresenter.
    //It is sent from ChangeLanguagePresenter.
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(ChangeLangEvent.To event) {
        bus.removeStickyEvent(event);
        Language language     = event.getLanguage();
        Language tempLanguage = languagePair.first;
        if (language.getCode().equals(tempLanguage.getCode())) {
            tempLanguage = languagePair.second;
        }
        languagePair = new Pair<>(tempLanguage, language);
        changeLangUpdate();
    }


    private void changeLangUpdate() {
        updateTranslationDirection();
        updateStorePair();
        translateText(translateText, true);
    }


    void clear() {
        getViewState().hideCleanBtn();
        getViewState().showHideFavoriteBtn(false);
        getViewState().showViewStub(TranslateFragmentState.IDLE, null);
    }


    Pair<Language, Language> getLanguagePair() {
        return languagePair;
    }


    private void unsubscribeTranslateSbs() {
        if (translateSbs != null) {
            translateSbs.unsubscribe();
        }
    }


    private TApplication getApplication() {
        if (application == null) {
            application = TApplication.getInstance();
        }
        return application;
    }


    private AppPreferences getPref() {
        if (pref == null || application == null) {
            pref = new AppPreferences(getApplication());
        }
        return pref;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        compositeSbs.unsubscribe();
        unsubscribeTranslateSbs();
        bus.unregister(this);
        bus        = null;
        repository = null;
    }
}
