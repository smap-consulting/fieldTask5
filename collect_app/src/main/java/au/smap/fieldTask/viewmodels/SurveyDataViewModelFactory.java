package au.smap.fieldTask.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.odk.collect.shared.settings.Settings;

public class SurveyDataViewModelFactory implements ViewModelProvider.Factory {

    private final Settings settings;

    public SurveyDataViewModelFactory(Settings settings) {
        this.settings = settings;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new SurveyDataViewModel(settings);
    }
}
