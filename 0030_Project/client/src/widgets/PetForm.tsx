import React, { useState } from 'react';
import { petSocket } from '../core/socket';

const PetForm: React.FC = () => {
  const [name, setName] = useState('');
  const [breed, setBreed] = useState('');
  const [ownerPhoneLast4, setOwnerPhoneLast4] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim() || !breed.trim() || !ownerPhoneLast4.trim()) {
      return;
    }

    if (!/^\d{4}$/.test(ownerPhoneLast4)) {
      alert('请输入4位数字的电话号码后四位');
      return;
    }

    setIsSubmitting(true);
    petSocket.addPet(name.trim(), breed.trim(), ownerPhoneLast4.trim());

    setName('');
    setBreed('');
    setOwnerPhoneLast4('');
    setIsSubmitting(false);
  };

  return (
    <div className="pet-form-panel">
      <div className="panel-header">
        <h3>新增寄养宠物</h3>
      </div>

      <form onSubmit={handleSubmit} className="pet-form">
        <div className="form-field">
          <label className="form-label">宠物名字</label>
          <input
            type="text"
            className="form-input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="比如：豆豆"
            disabled={isSubmitting}
          />
        </div>

        <div className="form-field">
          <label className="form-label">品种</label>
          <input
            type="text"
            className="form-input"
            value={breed}
            onChange={(e) => setBreed(e.target.value)}
            placeholder="比如：金毛、英短"
            disabled={isSubmitting}
          />
        </div>

        <div className="form-field">
          <label className="form-label">主人电话（后四位）</label>
          <input
            type="text"
            className="form-input"
            value={ownerPhoneLast4}
            onChange={(e) => {
              const val = e.target.value.replace(/\D/g, '').slice(0, 4);
              setOwnerPhoneLast4(val);
            }}
            placeholder="1234"
            maxLength={4}
            disabled={isSubmitting}
          />
        </div>

        <button type="submit" className="btn-primary btn-submit" disabled={isSubmitting}>
          🐾 登记入住
        </button>
      </form>
    </div>
  );
};

export default PetForm;
