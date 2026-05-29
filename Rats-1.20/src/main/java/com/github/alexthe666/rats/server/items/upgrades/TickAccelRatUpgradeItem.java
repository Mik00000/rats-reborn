package com.github.alexthe666.rats.server.items.upgrades;

import com.github.alexthe666.rats.server.entity.rat.TamedRat;
import com.github.alexthe666.rats.server.items.upgrades.interfaces.TickRatUpgrade;
import com.github.alexthe666.rats.server.misc.RatUtils;

public class TickAccelRatUpgradeItem extends BaseRatUpgradeItem implements TickRatUpgrade {
	public TickAccelRatUpgradeItem(Properties properties) {
		super(properties, 1, 2);
	}

	@Override
	public void tick(TamedRat rat) {
		if (rat.tickCount % 5 == 0) {
			RatUtils.accelerateTick(rat.level(), rat.blockPosition().above(), 0, 0);
			RatUtils.accelerateTick(rat.level(), rat.blockPosition(), 0, 0);
			RatUtils.accelerateTick(rat.level(), rat.blockPosition().below(), 0, 0);
		}
	}
}
