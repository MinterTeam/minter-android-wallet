package network.minter.bipwallet.apis.explorer;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import io.reactivex.Observable;
import network.minter.bipwallet.internal.data.CachedEntity;
import network.minter.bipwallet.internal.storage.KVStorage;
import network.minter.core.internal.api.ApiService;
import network.minter.explorer.models.ValidatorItem;
import network.minter.explorer.repo.ExplorerValidatorsRepository;

import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.rxExp;
import static network.minter.bipwallet.apis.reactive.ReactiveExplorer.toExpError;

public class CachedValidatorsRepository extends ExplorerValidatorsRepository implements CachedEntity<List<ValidatorItem>> {
    private final static String KEY_VALIDATORS = "cached_explorer_validators_repository_list";
    private KVStorage mStorage;

    public CachedValidatorsRepository(KVStorage storage, @Nonnull ApiService.Builder apiBuilder) {
        super(apiBuilder);
        mStorage = storage;
    }

    @Override
    public List<ValidatorItem> initialData() {
        return mStorage.get(KEY_VALIDATORS, Collections.emptyList());
    }

    @Override
    public Observable<List<ValidatorItem>> getUpdatableData() {
        return rxExp(getInstantService().getValidators())
                .onErrorResumeNext(toExpError())
                .map(result -> result.result);
    }

    @Override
    public void onAfterUpdate(List<ValidatorItem> result) {
        mStorage.put(KEY_VALIDATORS, result);
    }

    @Override
    public void onClear() {
        mStorage.delete(KEY_VALIDATORS);
    }
}
